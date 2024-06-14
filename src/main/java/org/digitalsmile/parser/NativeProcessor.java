package org.digitalsmile.parser;


import com.squareup.javapoet.JavaFile;
import org.digitalsmile.*;
import org.digitalsmile.Enum;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

@SupportedSourceVersion(SourceVersion.RELEASE_22)
public class NativeProcessor extends AbstractProcessor {

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(NativeMemory.class.getName(), Function.class.getName());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            return true;
        }
        for (Element element : roundEnv.getElementsAnnotatedWith(NativeMemory.class)) {
            var nativeAnnotation = element.getAnnotation(NativeMemory.class);
            var headerFile = nativeAnnotation.header();
            Path headerPath;
            if (headerFile.startsWith("/")) {
                headerPath = Path.of(headerFile);
            } else {
                try {
                    var rootPath = calculateRootPath();
                    headerPath = rootPath.resolve(headerFile);
                    if (!headerPath.toFile().exists()) {
                        var main = rootPath.resolve("src", "main");
                        var headerFileSplit = headerFile.split(Pattern.quote(File.separator));
                        headerPath = main.resolve("resources", headerFileSplit);
                    }
                } catch (IOException e) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Cannot find header file '" + headerFile + "' annotated in class '" + element + "'!");
                    return false;
                }
            }
            if (!headerPath.toFile().exists()) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Cannot find header file '" + headerFile + "' annotated in class '" + element + "'!");
                return false;
            }

            var structs = element.getAnnotation(Structs.class);
            var enums = element.getAnnotation(Enums.class);

            var parsedStructs = structs != null ? Arrays.stream(structs.value()).map(struct -> "S!" + struct.name() + ":" + struct.javaName()).toList() : Collections.<String>emptyList();
            var parsedEnums = enums != null ? Arrays.stream(enums.value()).map(enoom -> "E!" + enoom.name() + ":" + enoom.javaName()).toList() : Collections.<String>emptyList();

            var packageName = processingEnv.getElementUtils().getPackageOf(element).getQualifiedName().toString();
            List<String> args = new ArrayList<>();
            var runId = UUID.randomUUID().toString();
            args.add(headerPath.toFile().getAbsolutePath());
            args.add(packageName);
            args.add(runId);
            args.add(enums != null ? String.valueOf(nativeAnnotation.createEnumFromRootDefines()) : "false");
            args.addAll(parsedStructs);
            args.addAll(parsedEnums);
            processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, runId);
            try {
                parse(args);
                var tmpPath = System.getProperty("java.io.tmpdir");
                    var directory = Path.of(tmpPath, runId, packageName).toFile();
                    if (directory.exists()) {
                        var files = directory.listFiles();
                        if (files != null) {
                            for (File file : files) {
                                var outputFile = Files.readString(Path.of(file.getAbsolutePath()));
                                FileObject file1 = processingEnv.getFiler().createSourceFile(packageName + "." + file.getName());
                                Writer writer = file1.openWriter();
                                writer.write(outputFile);
                                writer.close();
                            }
                        }
                    }
            } catch (IOException | InterruptedException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
            }
            List<Element> elementList = new ArrayList<>(roundEnv.getElementsAnnotatedWith(Function.class));
            var javaName = element.getSimpleName().toString();

            try {
                var outputFile = FunctionComposer.compose(packageName, javaName, elementList, processingEnv.getTypeUtils());
                var file = processingEnv.getFiler().createSourceFile(packageName + "." + javaName + "Impl");
                Writer writer = file.openWriter();
                writer.write(outputFile);
                writer.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }


        return true;
    }

    private void parse(List<String> args) throws IOException, InterruptedException {
        var javaHome = System.getProperty("java.home");
        var javaBin = javaHome + File.separator + "bin" + File.separator + "java";
        List<String> command = new ArrayList<>();
        command.add(javaBin);
        command.add("--enable-native-access=ALL-UNNAMED");
        command.add("-cp");
        command.add(String.join(":",
                Parser.class.getProtectionDomain().getCodeSource().getLocation().getPath(),
                JavaFile.class.getProtectionDomain().getCodeSource().getLocation().getPath()
        ));
        command.add(Parser.class.getName());
        command.addAll(args);

        ProcessBuilder builder = new ProcessBuilder(command);
        Process process = builder.start();
        var output = new String(process.getInputStream().readAllBytes());
        var errors = new String(process.getErrorStream().readAllBytes());
        process.waitFor();
        if (!errors.isEmpty()) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Exit status: " + process.exitValue());
            var errorsSplit = errors.split("\n");
            for (String error : errorsSplit) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, error);
            }
        }
        if (!output.isEmpty()) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, output);
        }
    }

    private Path calculateRootPath() throws IOException {
        var resource = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", "tmp", (Element[]) null);
        var rootDirectory = calculatePath(new File(resource.toUri()));
        resource.delete();
        return rootDirectory.toPath();
    }

    private File calculatePath(File directory) {
        if (directory.isDirectory() && directory.getName().equals("build")) {
            return directory.getParentFile();
        }
        return calculatePath(directory.getParentFile());
    }
}

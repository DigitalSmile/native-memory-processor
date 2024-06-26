package io.github.digitalsmile;


import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeVariableName;
import io.github.digitalsmile.annotation.NativeMemory;
import io.github.digitalsmile.annotation.NativeMemoryOptions;
import io.github.digitalsmile.annotation.function.Function;
import io.github.digitalsmile.annotation.function.NativeMemoryException;
import io.github.digitalsmile.composers.FunctionComposer;
import io.github.digitalsmile.parser.Parser;
import io.github.digitalsmile.annotation.structure.Structs;
import io.github.digitalsmile.annotation.structure.Enums;
import io.github.digitalsmile.annotation.structure.NativeMemoryLayout;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.StandardLocation;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

@SupportedSourceVersion(SourceVersion.RELEASE_22)
public class NativeProcessor extends AbstractProcessor {

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(NativeMemory.class.getName(), Function.class.getName());
    }


    public record Library(String libraryName, boolean isAlreadyLoaded, Element element) {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Library library = (Library) o;
            return isAlreadyLoaded == library.isAlreadyLoaded && Objects.equals(libraryName, library.libraryName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(libraryName, isAlreadyLoaded);
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            return true;
        }
        for (Element rootElement : roundEnv.getElementsAnnotatedWith(NativeMemory.class)) {
            var nativeAnnotation = rootElement.getAnnotation(NativeMemory.class);
            var headerFile = nativeAnnotation.header();
            var packageName = processingEnv.getElementUtils().getPackageOf(rootElement).getQualifiedName().toString();
            var processedTypeNames = processHeaderFile(rootElement, headerFile, packageName, nativeAnnotation.options());
            List<Element> functionElements = new ArrayList<Element>(roundEnv.getElementsAnnotatedWith(Function.class)).stream()
                    .filter(f -> f.getEnclosingElement().equals(rootElement)).toList();
            Map<Library, Set<String>> libraries = new HashMap<>();
            for (Element functionElement : functionElements) {
                var function = functionElement.getAnnotation(Function.class);
                var library = new Library(function.library(), function.isAlreadyLoaded(), functionElement);
                var functions = libraries.getOrDefault(library, new HashSet<>());
                functions.add(function.name());
                libraries.putIfAbsent(library, functions);
                if (functionElement instanceof ExecutableElement functionParsedElement) {
                    var t = processingEnv.getElementUtils().getTypeElement(NativeMemoryException.class.getName()).asType();
                    if (!functionParsedElement.getThrownTypes().contains(t)) {
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Method '" + functionParsedElement + "' must throw NativeMemoryException!", functionParsedElement);
                        return false;
                    }
                }
            }
            for (Library libraryOuter : libraries.keySet()) {
                for (Library libraryInner : libraries.keySet()) {
                    if (libraryOuter.libraryName.equals(libraryInner.libraryName) && libraryOuter.isAlreadyLoaded != libraryInner.isAlreadyLoaded) {
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Library '" + libraryInner.libraryName + "' cannot be declared with different 'isAlreadyLoaded' option", libraryInner.element);
                        return false;
                    }
                }
            }
            //processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, libraries.toString());
            if (!functionElements.isEmpty()) {
                var javaName = rootElement.getSimpleName().toString();
                try {
                    var contents = FunctionComposer.compose(packageName, javaName, functionElements, libraries, processingEnv.getMessager(), processedTypeNames);
                    createGeneratedFile(packageName, javaName, "Native", contents);
                } catch (Exception e) {
                    e.printStackTrace();
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.toString());
                }
            }
        }


        return true;
    }

    private List<TypeVariableName> processHeaderFile(Element element, String headerFile, String packageName, NativeMemoryOptions options) {
        if (headerFile.isEmpty()) {
            return Collections.emptyList();
        }
        Path headerPath = getHeaderPath(headerFile);
        if (!headerPath.toFile().exists()) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Cannot find header file '" + headerFile + "' annotated in class '" + element + "'!");
            return Collections.emptyList();
        }

        var structs = element.getAnnotation(Structs.class);
        var enums = element.getAnnotation(Enums.class);

        var properties = new Properties();

        properties.put(ParsingOption.PACKAGE_NAME.getOption(), packageName);
        properties.put(ParsingOption.HEADER_FILE.getOption(), headerPath.toFile().getAbsolutePath());
        properties.put(ParsingOption.ROOT_ENUM_CREATION.getOption(), String.valueOf(options.generateRootEnum()));
        if (structs != null) {
            if (structs.value().length > 0) {
                Arrays.stream(structs.value()).forEach(struct -> properties.put("Structs." + struct.name(), struct.javaName()));
            } else {
                properties.put("Structs", "All");
            }
        }
        if (enums != null) {
            if (enums.value().length > 0) {
                Arrays.stream(enums.value()).forEach(struct -> properties.put("Enums." + struct.name(), struct.javaName()));
            } else {
                properties.put("Enums", "All");
            }
        }
        List<TypeVariableName> processedTypeNames = new ArrayList<>();
        try {
            var output = parse(properties);
            var files = output.split("===\n");
            for (String file : files) {
                //processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, file);
                var fileName = file.substring(0, file.indexOf("\n")).replace("fileName: ", "");
                file = file.substring(file.indexOf("\n") + 1);
                createGeneratedFile(packageName, fileName, file);
                processedTypeNames.add(TypeVariableName.get(fileName, TypeVariableName.get(NativeMemoryLayout.class)));
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "I/O exception occurred while trying to parse header file: " + e.getMessage());
        } catch (InterruptedException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Parsing process did not complete: " + e.getMessage());
        }
        return processedTypeNames;
    }


    private Path getHeaderPath(String headerFile) {
        Path headerPath;
        if (headerFile.startsWith("/")) {
            headerPath = Path.of(headerFile);
        } else {
            Path rootPath;
            try {
                var resource = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", "tmp", (Element[]) null);
                var rootDirectory = calculatePath(new File(resource.toUri()));
                resource.delete();
                rootPath = rootDirectory.toPath();
            } catch (IOException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Cannot create tmp resource " + e.getMessage());
                return Path.of("unknown");
            }
            headerPath = rootPath.resolve(headerFile);
            if (!headerPath.toFile().exists()) {
                var main = rootPath.resolve("src", "main");
                var headerFileSplit = headerFile.split(Pattern.quote(File.separator));
                headerPath = main.resolve("resources", headerFileSplit);
            }
        }
        return headerPath;
    }

    private File calculatePath(File directory) {
        if (directory.isDirectory() && directory.getName().equals("build")) {
            return directory.getParentFile();
        }
        return calculatePath(directory.getParentFile());
    }

    private void createGeneratedFile(String packageName, String fileName, String suffix, String contents) {
        try {
            var generatedFile = processingEnv.getFiler().createSourceFile(packageName + "." + fileName + suffix);
            var writer = generatedFile.openWriter();
            writer.write(contents);
            writer.close();
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Exception occurred while processing file '" + fileName + "': " + e.getMessage());
        }
    }

    private void createGeneratedFile(String packageName, String fileName, String contents) {
        createGeneratedFile(packageName, fileName, "", contents);
    }

    private String parse(Properties properties) throws IOException, InterruptedException {
        var javaHome = System.getProperty("java.home");
        var javaBin = javaHome + File.separator + "bin" + File.separator + "java";
        List<String> command = new ArrayList<>();
        command.add(javaBin);
        command.add("--enable-native-access=ALL-UNNAMED");
        command.add("-cp");
        command.add(String.join(":",
                NativeMemory.class.getProtectionDomain().getCodeSource().getLocation().getPath(),
                Parser.class.getProtectionDomain().getCodeSource().getLocation().getPath(),
                JavaFile.class.getProtectionDomain().getCodeSource().getLocation().getPath()
        ));
        command.add(Parser.class.getName());

        var builder = new ProcessBuilder(command);
        var process = builder.start();
        var writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
        properties.store(writer, "");
        writer.flush();
        writer.close();
        var output = new String(process.getInputStream().readAllBytes());
        var messages = new String(process.getErrorStream().readAllBytes());
        process.waitFor();

        if (!messages.isEmpty()) {
            var message = messages.split("\n");
            for (String line : message) {
                if (line.startsWith("Error:")) {
                    line = line.replace("Error:", "");
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, line);
                } else if (line.startsWith("Warning:")) {
                    line = line.replace("Warning:", "");
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, line);
                } else if (line.startsWith("Debug:")) {
                    line = line.replace("Debug:", "");
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, line);
                }
            }
        }
        return output;
    }
}

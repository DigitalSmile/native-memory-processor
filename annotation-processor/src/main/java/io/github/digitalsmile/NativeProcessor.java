package io.github.digitalsmile;


import io.avaje.prism.GeneratePrism;
import io.github.digitalsmile.annotation.NativeMemory;
import io.github.digitalsmile.annotation.NativeMemoryOptions;
import io.github.digitalsmile.annotation.function.ByAddress;
import io.github.digitalsmile.annotation.function.Function;
import io.github.digitalsmile.annotation.function.NativeMemoryException;
import io.github.digitalsmile.annotation.function.Returns;
import io.github.digitalsmile.annotation.structure.Enums;
import io.github.digitalsmile.annotation.structure.Structs;
import io.github.digitalsmile.annotation.structure.Unions;
import io.github.digitalsmile.composers.EnumComposer;
import io.github.digitalsmile.composers.FunctionComposer;
import io.github.digitalsmile.composers.StructComposer;
import io.github.digitalsmile.functions.FunctionNode;
import io.github.digitalsmile.functions.ParameterNode;
import io.github.digitalsmile.headers.mapping.ObjectTypeMapping;
import io.github.digitalsmile.headers.model.NativeMemoryModel;
import io.github.digitalsmile.headers.model.NativeMemoryNode;
import io.github.digitalsmile.headers.type.ObjectOriginalType;
import io.github.digitalsmile.headers.type.ObjectTypeMirror;
import io.github.digitalsmile.headers.type.OriginalType;
import io.github.digitalsmile.headers.Parser;
import org.openjdk.jextract.Declaration;
import org.openjdk.jextract.JextractTool;
import org.openjdk.jextract.clang.Index;
import org.openjdk.jextract.clang.TypeLayoutError;
import org.openjdk.jextract.impl.ClangException;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

@GeneratePrism(Function.class)
@SupportedSourceVersion(SourceVersion.RELEASE_22)
public class NativeProcessor extends AbstractProcessor {

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(NativeMemory.class.getName());
    }


    public record Library(String libraryName, boolean isAlreadyLoaded) {
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
            try {
                var nativeAnnotation = rootElement.getAnnotation(NativeMemory.class);
                var nativeOptions = rootElement.getAnnotation(NativeMemoryOptions.class);
                var headerFiles = nativeAnnotation.headers();
                var packageName = processingEnv.getElementUtils().getPackageOf(rootElement).getQualifiedName().toString();
                if (nativeOptions != null && !nativeOptions.packageName().isEmpty()) {
                    packageName = nativeOptions.packageName();
                }

                processHeaderFiles(rootElement, headerFiles, packageName, nativeOptions);

                List<Element> functionElements = new ArrayList<Element>(roundEnv.getElementsAnnotatedWith(Function.class)).stream()
                        .filter(f -> f.getEnclosingElement().equals(rootElement)).toList();
                processFunctions(rootElement, functionElements, packageName);

            } catch (Throwable e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
            }
        }
        return true;
    }

    public record Type(String name, String javaName) {
    }

    private void processHeaderFiles(Element element, String[] headerFiles, String packageName, NativeMemoryOptions options) {
        if (headerFiles.length == 0) {
            return;
        }
        List<Path> headerPaths = getHeaderPaths(headerFiles);
        for (Path path : headerPaths) {
            if (!path.toFile().exists()) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, "Cannot find header file '" + path + "'!", element);
                return;
            }
        }

        var structsAnnotation = element.getAnnotation(Structs.class);
        var unionsAnnotation = element.getAnnotation(Unions.class);
        var enumsAnnotation = element.getAnnotation(Enums.class);

        List<Type> structs = null;
        if (structsAnnotation != null) {
            structs = Arrays.stream(structsAnnotation.value()).map(struct -> new Type(struct.name(), struct.javaName())).toList();
            Arrays.stream(structsAnnotation.value()).forEach(struct -> {
                PrettyName.addName(struct.name(), struct.javaName());
                OriginalType.register(new ObjectTypeMapping(struct.name()));
            });
        }
        List<Type> enums = null;
        if (enumsAnnotation != null) {
            enums = Arrays.stream(enumsAnnotation.value()).map(enoom -> new Type(enoom.name(), enoom.javaName())).toList();
            Arrays.stream(enumsAnnotation.value()).forEach(enoom -> {
                PrettyName.addName(enoom.name(), enoom.javaName());
                OriginalType.register(new ObjectTypeMapping(enoom.name()));
            });
        }
        List<Type> unions = null;
        if (unionsAnnotation != null) {
            unions = Arrays.stream(unionsAnnotation.value()).map(union -> new Type(union.name(), union.javaName())).toList();
            Arrays.stream(unionsAnnotation.value()).forEach(union -> {
                PrettyName.addName(union.name(), union.javaName());
                OriginalType.register(new ObjectTypeMapping(union.name()));
            });
        }

        boolean rootConstants = false;
        List<String> includes = Collections.emptyList();
        if (options != null) {
            rootConstants = options.processRootConstants();
            includes = Arrays.stream(options.includes()).map(p -> "-I" + p).toList();
        }

        Map<Path, Declaration.Scoped> allParsedHeaders = new HashMap<>();

        for (Path header : headerPaths) {
            try {
                var parsed = JextractTool.parse(Path.of(header.toFile().getAbsolutePath()), includes);
                allParsedHeaders.put(header, parsed);
            } catch (ExceptionInInitializerError | ClangException | TypeLayoutError | Index.ParsingFailedException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
                return;
            }
        }

        var parser = new Parser(processingEnv.getMessager());
        try {
            var parsed = parser.parse(structs, enums, unions, allParsedHeaders);
            for (NativeMemoryModel model : parsed) {
                for (Map.Entry<Declaration.Scoped.Kind, List<NativeMemoryNode>> entry : model.nodes().entrySet()) {
                    var kind = entry.getKey();
                    switch (kind) {
                        case STRUCT -> processStructs(packageName, entry.getValue());
                        case ENUM -> processEnums(packageName, entry.getValue(), rootConstants);
                        case UNION -> processUnions(packageName, entry.getValue());
                    }
                }
            }
        } catch (Throwable e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
        }
    }

    private void processStructs(String packageName, List<NativeMemoryNode> nodes) {
        for (NativeMemoryNode node : nodes) {
            var structComposer = new StructComposer(processingEnv.getMessager());
            var output = structComposer.compose(packageName, PrettyName.getObjectName(node.getName()), node);
            createGeneratedFile(packageName, PrettyName.getObjectName(node.getName()), output);
        }
    }

    private void processEnums(String packageName, List<NativeMemoryNode> nodes, boolean rootConstants) {
        for (NativeMemoryNode node : nodes) {
            if (node.getName().contains("Constants")) {
                if (!rootConstants) {
                    continue;
                }
            }
            var enumComposer = new EnumComposer(processingEnv.getMessager());
            var output = enumComposer.compose(packageName, PrettyName.getObjectName(node.getName()), node);
            createGeneratedFile(packageName, PrettyName.getObjectName(node.getName()), output);
        }
    }

    private void processUnions(String packageName, List<NativeMemoryNode> nodes) {
        for (NativeMemoryNode node : nodes) {
            var structComposer = new StructComposer(processingEnv.getMessager());
            var output = structComposer.compose(packageName, PrettyName.getObjectName(node.getName()), node);
            createGeneratedFile(packageName, PrettyName.getObjectName(node.getName()), output);
        }
    }

    private void processFunctions(Element rootElement, List<Element> functionElements, String packageName) {
        Map<Library, List<FunctionNode>> libraries = new HashMap<>();
        Map<String, Boolean> loadedLibraries = new HashMap<>();
        Map<FunctionNode, ExecutableElement> methodMap = new HashMap<>();
        for (Element element : functionElements) {
            if (!(element instanceof ExecutableElement functionElement)) {
                continue;
            }
            var throwType = processingEnv.getElementUtils().getTypeElement(NativeMemoryException.class.getName()).asType();
            if (!functionElement.getThrownTypes().contains(throwType)) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Method '" + functionElement + "' must throw NativeMemoryException!", functionElement);
                break;
            }
            var instance = FunctionPrism.getInstanceOn(functionElement);
            if (instance == null) {
                break;
            }
            var loadedLibrary = loadedLibraries.computeIfAbsent(instance.library(), _ -> instance.isAlreadyLoaded());
            if (!loadedLibrary.equals(instance.isAlreadyLoaded())) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Library '" + instance.library() + "' cannot be declared with different 'isAlreadyLoaded' option", functionElement);
                break;
            }
            List<ParameterNode> parameters = new ArrayList<>();
            var returnType = OriginalType.of(functionElement.getReturnType());
            var nativeReturnType = OriginalType.of(instance.returnType() != null ? instance.returnType() : new ObjectTypeMirror());
            for (VariableElement variableElement : functionElement.getParameters()) {
                var returns = variableElement.getAnnotation(Returns.class);
                var byAddress = variableElement.getAnnotation(ByAddress.class);
                var type = OriginalType.of(variableElement.asType());
                var parameterNode = new ParameterNode(variableElement.getSimpleName().toString(),
                        type, returns != null, byAddress != null || type instanceof ObjectOriginalType);
                parameters.add(parameterNode);
            }

            var functionNode = new FunctionNode(instance.name(), nativeReturnType, returnType, parameters, instance.useErrno());
            var library = new Library(instance.library(), instance.isAlreadyLoaded());
            libraries.computeIfAbsent(library, _ -> new ArrayList<>()).add(functionNode);
            methodMap.put(functionNode, functionElement);
        }
        var functionComposer = new FunctionComposer(processingEnv.getMessager());

        var nativeClassJavaName = PrettyName.getObjectName(rootElement.getSimpleName().toString() + "Native");
        var originalJavaName = PrettyName.getObjectName(rootElement.getSimpleName().toString());
        var output = functionComposer.compose(packageName, originalJavaName, nativeClassJavaName, methodMap, libraries);
        createGeneratedFile(packageName, nativeClassJavaName, output);
    }

    private FileObject tmpFile;

    private List<Path> getHeaderPaths(String[] headerFiles) {
        List<Path> paths = new ArrayList<>();
        for (String headerFile : headerFiles) {
            Path headerPath;
            if (headerFile.startsWith("/")) {
                headerFile = headerFile.replace("${version}", System.getProperty("headerVersion"));
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, headerFile);
                headerPath = Path.of(headerFile);
            } else {
                Path rootPath;
                try {
                    if (tmpFile == null) {
                        tmpFile = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", "tmp", (Element[]) null);
                    }
                    var rootDirectory = calculatePath(new File(tmpFile.toUri()));
                    tmpFile.delete();

                    rootPath = rootDirectory.toPath();
                } catch (IOException e) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Cannot create tmp resource " + e);
                    continue;
                }
                headerPath = rootPath.resolve(headerFile);
                if (!headerPath.toFile().exists()) {
                    var main = rootPath.resolve("src", "main");
                    var headerFileSplit = headerFile.split(Pattern.quote(File.separator));
                    headerPath = main.resolve("resources", headerFileSplit);
                }
            }
            paths.add(headerPath);
        }
        return paths;
    }

    private File calculatePath(File directory) {
        if (directory.isDirectory() && directory.getName().equals("build")) {
            return directory.getParentFile();
        }
        return calculatePath(directory.getParentFile());
    }


    private void createGeneratedFile(String packageName, String fileName, String contents) {
        try {
            var generatedFile = processingEnv.getFiler().createSourceFile(packageName + "." + fileName);
            var writer = generatedFile.openWriter();
            writer.write(contents);
            writer.close();
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Exception occurred while processing file '" + fileName + "': " + e.getMessage());
        }
    }
}

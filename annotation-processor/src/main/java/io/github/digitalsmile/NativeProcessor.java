package io.github.digitalsmile;


import io.avaje.prism.GeneratePrism;
import io.github.digitalsmile.annotation.NativeMemory;
import io.github.digitalsmile.annotation.NativeMemoryException;
import io.github.digitalsmile.annotation.NativeMemoryOptions;
import io.github.digitalsmile.annotation.function.ByAddress;
import io.github.digitalsmile.annotation.function.NativeManualFunction;
import io.github.digitalsmile.annotation.function.Returns;
import io.github.digitalsmile.annotation.structure.Enums;
import io.github.digitalsmile.annotation.structure.Structs;
import io.github.digitalsmile.annotation.structure.Unions;
import io.github.digitalsmile.annotation.types.interfaces.NativeMemoryLayout;
import io.github.digitalsmile.composers.*;
import io.github.digitalsmile.functions.FunctionNode;
import io.github.digitalsmile.functions.FunctionOptions;
import io.github.digitalsmile.functions.Library;
import io.github.digitalsmile.functions.ParameterNode;
import io.github.digitalsmile.headers.Parser;
import io.github.digitalsmile.headers.mapping.ArrayOriginalType;
import io.github.digitalsmile.headers.mapping.FunctionOriginalType;
import io.github.digitalsmile.headers.mapping.ObjectOriginalType;
import io.github.digitalsmile.headers.mapping.OriginalType;
import io.github.digitalsmile.headers.model.NativeMemoryNode;
import io.github.digitalsmile.headers.model.NodeType;
import org.openjdk.jextract.Declaration;
import org.openjdk.jextract.JextractTool;
import org.openjdk.jextract.Position;
import org.openjdk.jextract.clang.Index;
import org.openjdk.jextract.clang.TypeLayoutError;
import org.openjdk.jextract.impl.ClangException;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@GeneratePrism(NativeManualFunction.class)
@SupportedSourceVersion(SourceVersion.RELEASE_22)
public class NativeProcessor extends AbstractProcessor {

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(NativeMemory.class.getName());
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
                PackageName.setDefaultPackageName(packageName);

                var parsed = processHeaderFiles(rootElement, headerFiles, packageName, nativeOptions);

                List<Element> functionElements = new ArrayList<Element>(roundEnv.getElementsAnnotatedWith(NativeManualFunction.class)).stream()
                        .filter(f -> f.getEnclosingElement().equals(rootElement)).toList();
                processFunctions(rootElement, functionElements, packageName, parsed);

            } catch (Throwable e) {
                printStackTrace(e);
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
            }
        }
        return true;
    }

    public record Type(String name, String javaName) {
    }

    private List<NativeMemoryNode> processHeaderFiles(Element element, String[] headerFiles, String packageName, NativeMemoryOptions options) {
        if (headerFiles.length == 0) {
            return Collections.emptyList();
        }
        List<Path> headerPaths = getHeaderPaths(headerFiles);
        for (Path path : headerPaths) {
            if (!path.toFile().exists()) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, "Cannot find header file '" + path + "'! Please, check file location", element);
                return Collections.emptyList();
            }
        }

        var structsAnnotation = element.getAnnotation(Structs.class);
        var unionsAnnotation = element.getAnnotation(Unions.class);
        var enumsAnnotation = element.getAnnotation(Enums.class);

        List<Type> structs = null;
        if (structsAnnotation != null) {
            structs = Arrays.stream(structsAnnotation.value()).map(struct -> new Type(struct.name(), struct.javaName())).toList();
        }
        List<Type> enums = null;
        if (enumsAnnotation != null) {
            enums = Arrays.stream(enumsAnnotation.value()).map(enoom -> new Type(enoom.name(), enoom.javaName())).toList();
        }
        List<Type> unions = null;
        if (unionsAnnotation != null) {
            unions = Arrays.stream(unionsAnnotation.value()).map(union -> new Type(union.name(), union.javaName())).toList();
        }

        boolean rootConstants = false;
        boolean debug = false;
        List<String> includes = Collections.emptyList();
        List<String> systemIncludes = Collections.emptyList();
        if (options != null) {
            rootConstants = options.processRootConstants();
            includes = Arrays.stream(options.includes()).map(p -> "-I" + p).toList();
            systemIncludes = Arrays.stream(options.systemIncludes()).map(p -> "-isystem" + p).toList();
            debug = options.debugMode();
        }

        Map<Path, Declaration.Scoped> allParsedHeaders = new HashMap<>();

        for (Path header : headerPaths) {
            try {
                var parsed = JextractTool.parse(Path.of(header.toFile().getAbsolutePath()), Stream.concat(includes.stream(), systemIncludes.stream()).toList());
                allParsedHeaders.put(header, parsed);
            } catch (ExceptionInInitializerError | ClangException | TypeLayoutError | Index.ParsingFailedException e) {
                if (debug) {
                    printStackTrace(e);
                }
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getCause().getMessage());
                return Collections.emptyList();
            }
        }
        var parser = new Parser(packageName, processingEnv.getMessager(), processingEnv.getFiler());
        try {
            var parsed = parser.parse(structs, enums, unions, allParsedHeaders, debug);
            for (NativeMemoryNode rootNode : parsed) {
                for (NativeMemoryNode node : rootNode.nodes()) {
                    switch (node.getNodeType()) {
                        case STRUCT -> processStructs(node);
                        case ENUM -> processEnums(node, rootConstants);
                        case UNION -> processUnions(node);
                        case OPAQUE -> processOpaque(node);
                    }
                }
            }
            return parsed;
        } catch (Throwable e) {
            if (debug) {
                printStackTrace(e);
            }
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
            return Collections.emptyList();
        }
    }

    private void processOpaque(NativeMemoryNode node) {
        var opaqueComposer = new OpaqueComposer(processingEnv.getMessager());
        var output = opaqueComposer.compose(PrettyName.getObjectName(node.getName()), node);
        createGeneratedFile(PackageName.getPackageName(node.getName()), PrettyName.getObjectName(node.getName()), output);
    }

    private void processStructs(NativeMemoryNode node) {
        if (node.nodes().stream().anyMatch(n -> n.getType() instanceof FunctionOriginalType)) {
            return;
        }
        var structComposer = new StructComposer(processingEnv.getMessager());
        var output = structComposer.compose(PrettyName.getObjectName(node.getName()), node);
        createGeneratedFile(PackageName.getPackageName(node.getName()), PrettyName.getObjectName(node.getName()), output);
    }

    private void processEnums(NativeMemoryNode node, boolean rootConstants) {
        if (node.getName().contains("Constants")) {
            if (!rootConstants) {
                return;
            }
        }
        if (node.nodes().isEmpty()) {
            return;
        }
        var enumComposer = new EnumComposer(processingEnv.getMessager());
        var output = enumComposer.compose(PrettyName.getObjectName(node.getName()), node);
        createGeneratedFile(PackageName.getPackageName(node.getName()), PrettyName.getObjectName(node.getName()), output);
    }

    private void processUnions(NativeMemoryNode node) {
        var structComposer = new StructComposer(processingEnv.getMessager(), true);
        var output = structComposer.compose(PrettyName.getObjectName(node.getName()), node);
        createGeneratedFile(PackageName.getPackageName(node.getName()), PrettyName.getObjectName(node.getName()), output);
    }

    private void processFunctions(Element rootElement, List<Element> functionElements, String packageName, List<NativeMemoryNode> parsed) {
        Map<Library, List<FunctionNode>> libraries = new HashMap<>();
        Map<String, Boolean> loadedLibraries = new HashMap<>();
        var flatten = flatten(parsed);
        List<FunctionNode> nodes = new ArrayList<>();
        for (Element element : functionElements) {
            if (!(element instanceof ExecutableElement functionElement)) {
                continue;
            }
            var throwType = processingEnv.getElementUtils().getTypeElement(NativeMemoryException.class.getName()).asType();
            if (!functionElement.getThrownTypes().contains(throwType)) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Method '" + functionElement + "' must throw NativeMemoryException!", functionElement);
                break;
            }
            var instance = NativeManualFunctionPrism.getInstanceOn(functionElement);
            if (instance == null) {
                break;
            }
            var loadedLibrary = loadedLibraries.computeIfAbsent(instance.library(), _ -> instance.isAlreadyLoaded());
            if (!loadedLibrary.equals(instance.isAlreadyLoaded())) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Library '" + instance.library() + "' cannot be declared with different 'isAlreadyLoaded' option", functionElement);
                break;
            }
            List<ParameterNode> parameters = new ArrayList<>();
            var returnType = OriginalType.of(processingEnv.getTypeUtils().erasure(functionElement.getReturnType()));
            var returnNode = flatten.stream().filter(p -> PrettyName.getObjectName(p.getName()).equals(returnType.typeName())).findFirst().orElse(null);
            if (returnNode == null) {
                var type = functionElement.getReturnType();
                var isReturnEnum = false;
                if (type instanceof DeclaredType declaredType) {
                    isReturnEnum = declaredType.asElement().getKind().equals(ElementKind.ENUM);
                }
                returnNode = new NativeMemoryNode("", isReturnEnum ? NodeType.ENUM : NodeType.STRUCT, returnType, 0, Position.NO_POSITION);
            }

            for (VariableElement variableElement : functionElement.getParameters()) {
                var variableName = variableElement.getSimpleName().toString();
                var returns = variableElement.getAnnotation(Returns.class);
                var byAddressAnnotation = variableElement.getAnnotation(ByAddress.class);
                var type = variableElement.asType();
                var node = flatten.stream().filter(p -> PrettyName.getObjectName(p.getName()).equals(type.toString()))
                        .findFirst().orElse(null);
                if (node == null) {
                    var isEnum = false;
                    if (type instanceof DeclaredType declaredType) {
                        isEnum = declaredType.asElement().getKind().equals(ElementKind.ENUM);
                    }
                    node = new NativeMemoryNode(variableName, isEnum ? NodeType.ENUM : NodeType.STRUCT, OriginalType.of(type), 0, Position.NO_POSITION);
                }
                var originalType = OriginalType.of(type);
                var byAddress = (byAddressAnnotation != null || originalType instanceof ObjectOriginalType || originalType instanceof ArrayOriginalType) && !node.getNodeType().isEnum();
                var parameterNode = new ParameterNode(variableName, node, returns != null, byAddress);
                parameters.add(parameterNode);
            }
            var functionOptions = new FunctionOptions(instance.name(), instance.isAlreadyLoaded(), instance.useErrno());
            var functionNode = new FunctionNode(functionElement.getSimpleName().toString(), functionOptions, returnNode, parameters, functionElement.getTypeParameters());
            nodes.add(functionNode);
            var libraryFileName = instance.library();
            if (instance.library().startsWith("/")) {
                libraryFileName = Path.of(instance.library()).toAbsolutePath().toFile().getName().split("\\.")[0];
            } else {
                libraryFileName = instance.library().split("\\.")[0];
            }
            var library = new Library(instance.library(), libraryFileName, instance.isAlreadyLoaded());
            libraries.computeIfAbsent(library, _ -> new ArrayList<>()).add(functionNode);
        }

        if (!nodes.isEmpty()) {
            Map<FunctionNode, String> nativeFunctionNames = new HashMap<>();
            var contextComposer = new ContextComposer(processingEnv.getMessager());
            var contextName = PrettyName.getObjectName(rootElement.getSimpleName().toString() + "Context");
            var output = contextComposer.compose(packageName, contextName, libraries, nativeFunctionNames);
            createGeneratedFile(packageName, contextName, output);

            var functionComposer = new FunctionComposer(processingEnv.getMessager());
            var originalJavaName = PrettyName.getObjectName(rootElement.getSimpleName().toString());
            output = functionComposer.compose(packageName, originalJavaName, nodes, nativeFunctionNames);
            createGeneratedFile(packageName, originalJavaName + "Native", output);
        }
    }

    private List<NativeMemoryNode> flatten(List<NativeMemoryNode> nodes) {
        return nodes.stream().flatMap(i -> Stream.concat(Stream.of(i), flatten(i.nodes()).stream())).toList();
    }

    private OriginalType getBoundsOriginalType(ExecutableElement functionElement) {
        var methodRealTypes = functionElement.getTypeParameters().stream().findFirst().orElse(null);
        if (methodRealTypes != null) {
            var boundsType = methodRealTypes.getBounds().stream().findFirst().orElse(null);
            if (boundsType == null || !boundsType.toString().equals(NativeMemoryLayout.class.getName())) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Parameter with object definition should extend NativeMemoryLayout interface", functionElement);
                return null;
            } else {
                return OriginalType.of(boundsType);
            }
        } else {
            return OriginalType.of(functionElement.getReturnType());
        }
    }

    private FileObject tmpFile;

    private List<Path> getHeaderPaths(String[] headerFiles) {
        List<Path> paths = new ArrayList<>();
        for (String headerFile : headerFiles) {
            var beginVariable = headerFile.indexOf("${");
            var endVariable = headerFile.indexOf("}");
            if (beginVariable != -1 && endVariable != -1) {
                var sub = headerFile.substring(beginVariable + 2, endVariable);
                var property = System.getProperty(sub);
                if (property != null) {
                    headerFile = headerFile.replace("${" + sub + "}", property);
                } else {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "'" + headerFile + "' contains system property '" + sub + "', which is no defined");
                    continue;
                }
            }
            Path headerPath;
            if (headerFile.startsWith("/")) {
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
                    if (!headerPath.toFile().exists()) {
                        var test = rootPath.resolve("src", "test");
                        headerFileSplit = headerFile.split(Pattern.quote(File.separator));
                        headerPath = test.resolve("resources", headerFileSplit);
                    }
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

    private void printStackTrace(Throwable exception) {
        var writer = new StringWriter();
        var printWriter = new PrintWriter(writer);
        exception.printStackTrace(printWriter);
        printWriter.flush();

        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, writer.toString());
    }
}

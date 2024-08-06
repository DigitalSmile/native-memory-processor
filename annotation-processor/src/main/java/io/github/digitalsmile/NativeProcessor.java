package io.github.digitalsmile;


import io.avaje.prism.GeneratePrism;
import io.github.digitalsmile.annotation.ArenaType;
import io.github.digitalsmile.annotation.NativeMemory;
import io.github.digitalsmile.annotation.NativeMemoryOptions;
import io.github.digitalsmile.annotation.function.ByAddress;
import io.github.digitalsmile.annotation.function.NativeManualFunction;
import io.github.digitalsmile.annotation.function.Returns;
import io.github.digitalsmile.annotation.library.NativeMemoryLibrary;
import io.github.digitalsmile.annotation.structure.*;
import io.github.digitalsmile.annotation.structure.Enum;
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
import io.github.digitalsmile.validation.NativeProcessorValidator;
import io.github.digitalsmile.validation.ValidationException;
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

import static java.util.Collections.emptyList;

@GeneratePrism(NativeManualFunction.class)
@SupportedSourceVersion(SourceVersion.RELEASE_22)
public class NativeProcessor extends AbstractProcessor {

    private NativeProcessorValidator validator;


    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(NativeMemory.class.getName(), NativeMemoryLibrary.class.getName(), NativeManualFunction.class.getName());
    }


    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            return true;
        }
        this.validator = new NativeProcessorValidator(processingEnv.getMessager(), processingEnv.getElementUtils(), processingEnv.getTypeUtils());

        for (Element rootElement : roundEnv.getRootElements()) {
            if (!rootElement.getKind().isInterface()) {
                continue;
            }
            var nativeMemory = rootElement.getAnnotation(NativeMemory.class);
            var manualFunctionElements = rootElement.getEnclosedElements().stream().filter(p -> p.getAnnotation(NativeManualFunction.class) != null).toList();
            var automaticFunctionElements = rootElement.getAnnotation(NativeMemoryLibrary.class);
            if (nativeMemory == null && manualFunctionElements.isEmpty() && automaticFunctionElements == null) {
                continue;
            }

            try {
                var parsed = Collections.<NativeMemoryNode>emptyList();
                var packageName = processingEnv.getElementUtils().getPackageOf(rootElement).getQualifiedName().toString();
                var nativeOptions = rootElement.getAnnotation(NativeMemoryOptions.class);
                if (nativeOptions != null && !nativeOptions.packageName().isEmpty()) {
                    packageName = validator.validatePackageName(nativeOptions.packageName());
                }
                PackageName.setDefaultPackageName(packageName);


                if (nativeMemory != null) {
                    var headerFiles = nativeMemory.headers();
                    parsed = processHeaderFiles(rootElement, headerFiles, packageName, nativeOptions);
                }

                List<Element> manualFunctions = new ArrayList<>();
                for (Element manualFunction : manualFunctionElements) {
                    validator.validateManualFunction(manualFunction);
                    manualFunctions.add(manualFunction);
                }

                if (automaticFunctionElements != null) {
                    validator.validateAutomaticFunctions(parsed, automaticFunctionElements);
                }

                processFunctions(rootElement, manualFunctions, packageName, parsed, nativeOptions);
            } catch (ValidationException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage(), e.getElement());
            } catch (Throwable e) {
                printStackTrace(e);
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage(), rootElement);
            }
        }
        return true;
    }

    private List<NativeMemoryNode> processHeaderFiles(Element element, String[] headerFiles, String packageName, NativeMemoryOptions options) throws ValidationException {
        if (headerFiles.length == 0) {
            return emptyList();
        }
        List<Path> headerPaths = getHeaderPaths(headerFiles);
        for (Path path : headerPaths) {
            if (!path.toFile().exists()) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, "Cannot find header file '" + path + "'! Please, check file location", element);
                return emptyList();
            }
        }

        var structsAnnotation = element.getAnnotation(Structs.class);
        var unionsAnnotation = element.getAnnotation(Unions.class);
        var enumsAnnotation = element.getAnnotation(Enums.class);

        List<String> structs = null;
        if (structsAnnotation != null) {
            structs = Arrays.stream(structsAnnotation.value()).map(Struct::name).toList();
            for (Struct struct : structsAnnotation.value()) {
                var javaName = validator.validateJavaName(struct.javaName());
                PrettyName.addName(struct.name(), javaName);
            }
        }
        List<String> enums = null;
        if (enumsAnnotation != null) {
            enums = Arrays.stream(enumsAnnotation.value()).map(Enum::name).toList();
            for (Enum enoom : enumsAnnotation.value()) {
                var javaName = validator.validateJavaName(enoom.javaName());
                PrettyName.addName(enoom.name(), javaName);
            }
        }
        List<String> unions = null;
        if (unionsAnnotation != null) {
            unions = Arrays.stream(unionsAnnotation.value()).map(Union::name).toList();
            for (Union union : unionsAnnotation.value()) {
                var javaName = validator.validateJavaName(union.javaName());
                PrettyName.addName(union.name(), javaName);
            }
        }

        var rootConstants = false;
        var debug = false;
        var systemHeader = false;
        List<String> includes = emptyList();
        List<String> systemIncludes = emptyList();
        if (options != null) {
            rootConstants = options.processRootConstants();
            includes = getHeaderPaths(options.includes()).stream().map(p -> "-I" + p.toFile().getAbsolutePath()).toList();
            systemIncludes = getHeaderPaths(options.systemIncludes()).stream().map(p -> "-isystem" + p.toFile().getAbsolutePath()).toList();
            debug = options.debugMode();
            systemHeader = options.systemHeader();
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
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
                return emptyList();
            }
        }
        var parser = new Parser(packageName, processingEnv.getMessager(), processingEnv.getFiler());
        try {
            var parsed = parser.parse(structs, enums, unions, allParsedHeaders, debug, systemHeader);
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
            //if (debug) {
            printStackTrace(e);
            //}
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
            return emptyList();
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
        var name = node.getName();
        if (node.getName().endsWith("_constants")) {
            if (!rootConstants) {
                return;
            }
            name = name.substring(name.lastIndexOf("/"), name.lastIndexOf(".")) + "_constants";
        }
        var enumComposer = new EnumComposer(processingEnv.getMessager());
        var output = enumComposer.compose(PrettyName.getObjectName(name), node);
        createGeneratedFile(PackageName.getPackageName(name), PrettyName.getObjectName(name), output);
    }

    private void processUnions(NativeMemoryNode node) {
        var structComposer = new StructComposer(processingEnv.getMessager(), true);
        var output = structComposer.compose(PrettyName.getObjectName(node.getName()), node);
        createGeneratedFile(PackageName.getPackageName(node.getName()), PrettyName.getObjectName(node.getName()), output);
    }

    private void processFunctions(Element rootElement, List<Element> functionElements, String packageName, List<NativeMemoryNode> parsed, NativeMemoryOptions nativeOptions) {
        Map<Library, List<FunctionNode>> libraries = new HashMap<>();
        Map<String, Boolean> loadedLibraries = new HashMap<>();
        var flatten = flatten(parsed);
        List<FunctionNode> nodes = new ArrayList<>();
        var arentType = ArenaType.AUTO;
        if (nativeOptions != null) {
            arentType = nativeOptions.arena();
        }
        for (Element element : functionElements) {
            if (!(element instanceof ExecutableElement functionElement)) {
                continue;
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
            var returnType = OriginalType.of(functionElement.getReturnType());
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


        Map<FunctionNode, String> nativeFunctionNames = new HashMap<>();
        var contextComposer = new ContextComposer(processingEnv.getMessager());
        var contextName = PrettyName.getObjectName(rootElement.getSimpleName().toString() + "Context");
        var output = contextComposer.compose(packageName, contextName, libraries, nativeFunctionNames, arentType);
        createGeneratedFile(packageName, contextName, output);

        if (!nodes.isEmpty()) {
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

    private List<Path> getHeaderPaths(String... headerFiles) throws ValidationException {
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
            headerFile = validator.validatePath(headerFile);
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

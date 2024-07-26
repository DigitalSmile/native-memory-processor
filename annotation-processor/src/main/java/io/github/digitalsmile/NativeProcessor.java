package io.github.digitalsmile;


import io.avaje.prism.GeneratePrism;
import io.github.digitalsmile.annotation.NativeMemory;
import io.github.digitalsmile.annotation.NativeMemoryOptions;
import io.github.digitalsmile.annotation.function.ByAddress;
import io.github.digitalsmile.annotation.function.NativeFunction;
import io.github.digitalsmile.annotation.function.NativeMemoryException;
import io.github.digitalsmile.annotation.function.Returns;
import io.github.digitalsmile.annotation.structure.Enums;
import io.github.digitalsmile.annotation.structure.NativeMemoryLayout;
import io.github.digitalsmile.annotation.structure.Structs;
import io.github.digitalsmile.annotation.structure.Unions;
import io.github.digitalsmile.composers.EnumComposer;
import io.github.digitalsmile.composers.FunctionComposer;
import io.github.digitalsmile.composers.OpaqueComposer;
import io.github.digitalsmile.composers.StructComposer;
import io.github.digitalsmile.functions.FunctionNode;
import io.github.digitalsmile.functions.Library;
import io.github.digitalsmile.functions.ParameterNode;
import io.github.digitalsmile.headers.Parser;
import io.github.digitalsmile.headers.mapping.FunctionOriginalType;
import io.github.digitalsmile.headers.mapping.ObjectOriginalType;
import io.github.digitalsmile.headers.mapping.ObjectTypeMirror;
import io.github.digitalsmile.headers.mapping.OriginalType;
import io.github.digitalsmile.headers.model.NativeMemoryNode;
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
import javax.lang.model.type.TypeKind;
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

@GeneratePrism(NativeFunction.class)
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

                processHeaderFiles(rootElement, headerFiles, packageName, nativeOptions);

                List<Element> functionElements = new ArrayList<Element>(roundEnv.getElementsAnnotatedWith(NativeFunction.class)).stream()
                        .filter(f -> f.getEnclosingElement().equals(rootElement)).toList();
                processFunctions(rootElement, functionElements, packageName);

            } catch (Throwable e) {
                e.printStackTrace();
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
                processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, "Cannot find header file '" + path + "'! Please, check file location", element);
                return;
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
                return;
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
        } catch (Throwable e) {
            if (debug) {
                printStackTrace(e);
            }
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
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
            var instance = NativeFunctionPrism.getInstanceOn(functionElement);
            if (instance == null) {
                break;
            }
            var loadedLibrary = loadedLibraries.computeIfAbsent(instance.library(), _ -> instance.isAlreadyLoaded());
            if (!loadedLibrary.equals(instance.isAlreadyLoaded())) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Library '" + instance.library() + "' cannot be declared with different 'isAlreadyLoaded' option", functionElement);
                break;
            }
            List<ParameterNode> parameters = new ArrayList<>();

            var returnType = getBoundsOriginalType(functionElement);
            if (returnType == null) {
                return;
            }
            var nativeReturnType = OriginalType.of(instance.returnType() != null ? instance.returnType() : new ObjectTypeMirror());
            for (VariableElement variableElement : functionElement.getParameters()) {
                var returns = variableElement.getAnnotation(Returns.class);
                var byAddress = variableElement.getAnnotation(ByAddress.class);
                var type = variableElement.asType();
                var originalType = type.getKind().equals(TypeKind.TYPEVAR) ? returnType : OriginalType.of(type);
                var parameterNode = new ParameterNode(variableElement.getSimpleName().toString(),
                        originalType, returns != null, byAddress != null || originalType instanceof ObjectOriginalType);
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

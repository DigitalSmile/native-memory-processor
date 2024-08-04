package io.github.digitalsmile.composers;

import com.squareup.javapoet.*;
import io.github.digitalsmile.PackageName;
import io.github.digitalsmile.PrettyName;
import io.github.digitalsmile.annotation.ArenaType;
import io.github.digitalsmile.annotation.function.NativeCall;
import io.github.digitalsmile.annotation.types.interfaces.NativeMemoryContext;
import io.github.digitalsmile.functions.FunctionNode;
import io.github.digitalsmile.functions.Library;
import io.github.digitalsmile.functions.ParameterNode;
import io.github.digitalsmile.headers.mapping.ArrayOriginalType;
import io.github.digitalsmile.headers.mapping.ObjectOriginalType;
import io.github.digitalsmile.headers.mapping.PrimitiveOriginalType;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Modifier;
import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ContextComposer {
    private final Messager messager;

    public ContextComposer(Messager messager) {
        this.messager = messager;
    }

    public String compose(String packageName, String javaName, Map<Library, List<FunctionNode>> libraries, Map<FunctionNode, String> nativeFunctionNames, ArenaType arenaType) {
        var classBuilder = TypeSpec.classBuilder(javaName)
                .addSuperinterface(NativeMemoryContext.class);
        classBuilder.addField(FieldSpec.builder(Arena.class, "ARENA")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("$T.$L", Arena.class, arenaType.arena())
                .build());
        List<String> nameCache = new ArrayList<>();
        var counter = 0;
        for (Library libraryEntry : libraries.keySet()) {
            var libraryBuilder = CodeBlock.builder();
            if (libraryEntry.libraryName().equals("libc")) {
                libraryBuilder.add("$T.nativeLinker().defaultLookup()", Linker.class);
            } else {
                if (libraryEntry.isAlreadyLoaded()) {
                    libraryBuilder.add("$T.loaderLookup()", SymbolLookup.class);
                } else if (libraryEntry.libraryName().startsWith("/")) {
                    libraryBuilder.add("$T.libraryLookup($T.of($S), ARENA)", SymbolLookup.class, Path.class, libraryEntry.libraryName());
                } else {
                    libraryBuilder.add("$T.libraryLookup($S, ARENA)", SymbolLookup.class, libraryEntry.libraryName());
                }
            }
            var libFieldName = libraryEntry.libraryFilename().toUpperCase() + "_LIB";
            if (nameCache.stream().anyMatch(p -> p.equals(libraryEntry.libraryFilename().toUpperCase() + "_LIB"))) {
                libFieldName = libFieldName + "_" + counter++;
            } else {
                nameCache.add(libFieldName);
            }
            classBuilder.addField(FieldSpec.builder(SymbolLookup.class,
                            libFieldName,
                            Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .initializer(libraryBuilder.build())
                    .build());

            for (FunctionNode functionNode : libraries.get(libraryEntry)) {
                List<CodeBlock> parameters = new ArrayList<>();
                var options = functionNode.functionOptions();
                var returnNode = functionNode.returnNode();
                var returnType = returnNode.getType();
                if (!returnType.carrierClass().equals(void.class)) {
                    switch (returnType) {
                        case PrimitiveOriginalType primitiveTypeMapping ->
                                parameters.add(CodeBlock.builder().add("$T.$L", ValueLayout.class, primitiveTypeMapping.valueLayoutName()).build());
                        case ObjectOriginalType _, ArrayOriginalType _ -> {
                            if (returnNode.getNodeType().isEnum()) {
                                parameters.add(CodeBlock.builder().add("$T.JAVA_INT", ValueLayout.class).build());
                            } else {
                                parameters.add(CodeBlock.builder().add("$T.ADDRESS", ValueLayout.class).build());
                            }
                        }
                        default -> throw new IllegalStateException("Unexpected value: " + returnType);
                    }
                }
                for (ParameterNode parameterNode : functionNode.functionParameters()) {
                    var node = parameterNode.nativeMemoryNode();
                    var type = node.getType();
                    if (parameterNode.byAddress()) {
                        parameters.add(CodeBlock.builder().add("$T.ADDRESS", ValueLayout.class).build());
                    } else if (node.getNodeType().isEnum()) {
                        var parameterPackageName = PackageName.getPackageName(type.typeName());
                        var typeName = ClassName.get(parameterPackageName, PrettyName.getObjectName(type.typeName()));
                        parameters.add(CodeBlock.builder().add("$T.LAYOUT", typeName).build());
                    } else {
                        parameters.add(CodeBlock.builder().add("$T.$L", ValueLayout.class, type.valueLayoutName()).build());
                    }
                }
                var initializer = CodeBlock.builder().add("$T.nativeLinker().downcallHandle($W", Linker.class);
                initializer.add("$L.find($S).orElseThrow(),$W", libraryEntry.libraryFilename().toUpperCase() + "_LIB", options.nativeFunctionName());

                initializer.add("$T.$L($L)$L", FunctionDescriptor.class,
                        returnType.carrierClass().equals(void.class) ? "ofVoid" : "of",
                        CodeBlock.join(parameters, ", "), options.useErrno() ? "," : ")");

                if (options.useErrno()) {
                    initializer.add("$W");
                    initializer.add("$T.captureCallState($S))", Linker.Option.class, "errno");
                }
                var functionName = options.nativeFunctionName().toUpperCase();
                if (nameCache.stream().anyMatch(p -> p.equals(options.nativeFunctionName().toUpperCase()))) {
                    functionName = functionName + "_" + counter++;
                } else {
                    nameCache.add(functionName);
                }
                nativeFunctionNames.put(functionNode, functionName);
                classBuilder.addField(FieldSpec.builder(MethodHandle.class,
                                functionName,
                                Modifier.STATIC, Modifier.FINAL)
                        .initializer(initializer.build())
                        .build());
            }
        }

        classBuilder.addMethod(MethodSpec.methodBuilder("allocate")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(AnnotationSpec.builder(Override.class).build())
                        .addParameter(long.class, "byteSize")
                        .addParameter(long.class, "byteAlignment")
                        .returns(MemorySegment.class)
                        .addCode(CodeBlock.builder().addStatement("return ARENA.allocate(byteSize, byteAlignment)").build())
                        .build())
                .addMethod(MethodSpec.methodBuilder("checkIsCreatedByArena")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(AnnotationSpec.builder(Override.class).build())
                        .addParameter(MemorySegment.class, "segment")
                        .addCode(CodeBlock.builder()
                                .beginControlFlow("if ((!ARENA.scope().equals(segment.scope()) || !$T.createdInContext(segment.scope())) && !Arena.global().scope().equals(segment.scope()))",
                                        NativeCall.class)
                                .addStatement("throw new IllegalArgumentException(\"The scope of the MemorySegment arena is not the same as the scope of the arena\")")
                                .endControlFlow()
                                .build())
                        .build())
                .addMethod(MethodSpec.methodBuilder("getArena")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(AnnotationSpec.builder(Override.class).build())
                        .returns(Arena.class)
                        .addCode(CodeBlock.builder().addStatement("return ARENA").build())
                        .build())
                .addMethod(MethodSpec.methodBuilder("close")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(AnnotationSpec.builder(Override.class).build())
                        .addCode(CodeBlock.builder().addStatement("ARENA.close()").build())
                        .build());

        var builder = JavaFile.builder(packageName, classBuilder.build());
        var outputFile = builder.indent("\t").skipJavaLangImports(true).build();
        return outputFile.toString();
    }
}

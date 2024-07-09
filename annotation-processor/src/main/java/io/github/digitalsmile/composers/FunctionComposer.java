package io.github.digitalsmile.composers;

import com.squareup.javapoet.*;
import io.github.digitalsmile.functions.Library;
import io.github.digitalsmile.PrettyName;
import io.github.digitalsmile.annotation.function.NativeCall;
import io.github.digitalsmile.annotation.function.NativeMemoryException;
import io.github.digitalsmile.functions.FunctionNode;
import io.github.digitalsmile.functions.ParameterNode;
import io.github.digitalsmile.headers.mapping.DeferredObjectTypeMapping;
import io.github.digitalsmile.headers.mapping.ObjectTypeMapping;
import io.github.digitalsmile.headers.mapping.PrimitiveTypeMapping;
import io.github.digitalsmile.headers.type.ArrayOriginalType;
import io.github.digitalsmile.headers.type.ObjectOriginalType;
import io.github.digitalsmile.headers.type.PrimitiveOriginalType;

import javax.annotation.processing.Messager;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FunctionComposer {
    private final Messager messager;

    public FunctionComposer(Messager messager) {
        this.messager = messager;
    }

    public String compose(String packageName, String originalName, String javaName, Map<FunctionNode, ExecutableElement> methodsMap, Map<Library, List<FunctionNode>> libraries) {
        var classBuilder = TypeSpec.classBuilder(javaName)
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(ClassName.get(packageName, originalName))
                .superclass(NativeCall.class);

        Map<FunctionNode, String> nameMapper = new HashMap<>();
        for (Library libraryEntry : libraries.keySet()) {
            var counter = 0;
            for (FunctionNode node : libraries.get(libraryEntry)) {
                var contains = nameMapper.containsValue(node.functionName());
                nameMapper.put(node, !contains ? node.functionName().toUpperCase() + counter++ : node.functionName().toUpperCase());
            }

            if (libraryEntry.libraryName().equals("libc")) {
                continue;
            }
            var initializer = CodeBlock.builder();
            if (libraryEntry.isAlreadyLoaded()) {
                initializer.add("$T.loaderLookup()", SymbolLookup.class);
            } else if (libraryEntry.libraryName().startsWith("/")) {
                initializer.add("$T.libraryLookup(Path.of($S), Arena.global())", SymbolLookup.class, libraryEntry.libraryName());
            } else {
                initializer.add("$T.libraryLookup($S, Arena.global())", SymbolLookup.class, libraryEntry.libraryName());
            }
            classBuilder.addField(FieldSpec.builder(SymbolLookup.class,
                            libraryEntry.libraryName().toUpperCase() + "_LIB",
                            Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .initializer(initializer.build())
                    .build());
        }
        for (List<FunctionNode> nodes : libraries.values()) {
            for (FunctionNode functionNode : nodes) {
                var returnsCodeBlock = CodeBlock.builder();
                List<CodeBlock> parameters = new ArrayList<>();
                var nativeReturnTypeMapping = functionNode.nativeReturnType().typeMapping();
                if (!nativeReturnTypeMapping.carrierClass().equals(void.class)) {
                    switch (nativeReturnTypeMapping) {
                        case PrimitiveTypeMapping primitiveTypeMapping ->
                                parameters.add(CodeBlock.builder().add("$T.$L", ValueLayout.class, primitiveTypeMapping.valueLayoutName()).build());
                        case ObjectTypeMapping _, DeferredObjectTypeMapping _ ->
                                parameters.add(CodeBlock.builder().add("$T.ADDRESS", ValueLayout.class).build());
                        default -> throw new IllegalStateException("Unexpected value: " + nativeReturnTypeMapping);
                    }
                }
                List<CodeBlock> arguments = new ArrayList<>();
                var methodBody = CodeBlock.builder();
                for (ParameterNode parameterNode : functionNode.functionParameters()) {
                    var prettyName = PrettyName.getVariableName(parameterNode.name());
                    var parameterTypeMapping = parameterNode.typeMapping().typeMapping();
                    if (parameterNode.byAddress()) {
                        parameters.add(CodeBlock.builder().add("$T.ADDRESS", ValueLayout.class).build());
                        arguments.add(CodeBlock.builder().add("$LMemorySegment", prettyName).build());

                        switch (parameterNode.typeMapping()) {
                            case ArrayOriginalType _ -> {
                                methodBody.addStatement("var $LMemorySegment = offHeap.allocateFrom($T.$L, $L)", prettyName,
                                        ValueLayout.class, parameterTypeMapping.valueLayoutName(), prettyName);
                            }
                            case ObjectOriginalType _ -> {
                                if (parameterTypeMapping.carrierClass().equals(String.class)) {
                                    methodBody.addStatement("var $LMemorySegment = offHeap.allocateFrom($L)", prettyName, prettyName);
                                } else {
                                    methodBody.addStatement("var $LMemorySegment = offHeap.allocate($L.getMemoryLayout())", prettyName, prettyName);
                                    methodBody.addStatement("$L.toBytes($LMemorySegment)", prettyName, prettyName);
                                }
                            }
                            case PrimitiveOriginalType _ -> {
                                methodBody.addStatement("var $LMemorySegment = offHeap.allocate($T.$L)", prettyName, ValueLayout.class, parameterTypeMapping.valueLayoutName());
                                methodBody.addStatement("$LMemorySegment.set($T.$L, 0, $L)", prettyName, ValueLayout.class, parameterTypeMapping.valueLayoutName(), prettyName);
                            }
                            default ->
                                    throw new IllegalStateException("Unexpected value: " + parameterNode.typeMapping());
                        }
                    } else {
                        parameters.add(CodeBlock.builder().add("$T.$L", ValueLayout.class, parameterTypeMapping.valueLayoutName()).build());
                        if (parameterTypeMapping instanceof ObjectTypeMapping) {
                            arguments.add(CodeBlock.builder().add("$LMemorySegment", prettyName).build());
                        } else {
                            arguments.add(CodeBlock.builder().add("$L", prettyName).build());
                        }
                    }

                    if (parameterNode.returns()) {
                        switch (parameterNode.typeMapping()) {
                            case ArrayOriginalType _ -> {
                                returnsCodeBlock.addStatement("return $LMemorySegment.toArray($T.$L)", prettyName, ValueLayout.class, parameterTypeMapping.valueLayoutName());
                            }
                            case ObjectOriginalType _ -> {
                                if (parameterTypeMapping.carrierClass().equals(String.class)) {
                                    returnsCodeBlock.addStatement("return $LMemorySegment.getString(0)", prettyName);
                                } else {
                                    returnsCodeBlock.addStatement("return $L.createEmpty().fromBytes($LMemorySegment)", PrettyName.getObjectName(functionNode.returnType().typeName()), prettyName);
                                }
                            }
                            case PrimitiveOriginalType _ -> {
                                returnsCodeBlock.addStatement("return $LMemorySegment.get($T.$L, 0)", prettyName, ValueLayout.class, parameterTypeMapping.valueLayoutName());
                            }
                            default ->
                                    throw new IllegalStateException("Unexpected value: " + parameterNode.typeMapping());
                        }
                    }
                }

                if (!nativeReturnTypeMapping.carrierClass().equals(void.class) && functionNode.functionParameters().stream().noneMatch(ParameterNode::returns)) {
                    returnsCodeBlock.addStatement("return callResult");
                }

                CodeBlock.Builder initializer = CodeBlock.builder().add("$T.nativeLinker().downcallHandle($W", Linker.class);
                var library = libraries.entrySet().stream().filter(e -> e.getValue().contains(functionNode)).map(Map.Entry::getKey).findFirst().orElseThrow();
                var libraryName = library.libraryName().equals("libc") ? "STD" : library.libraryName().toUpperCase();
                initializer.add("$L.find($S).orElseThrow(),$W", libraryName + "_LIB", functionNode.functionName());

                initializer.add("$T.$L($L)$L", FunctionDescriptor.class,
                        nativeReturnTypeMapping.carrierClass().equals(void.class) ? "ofVoid" : "of",
                        CodeBlock.join(parameters, ", "), functionNode.useErrno() ? "," : ")");

                if (functionNode.useErrno()) {
                    initializer.add("$W");
                    initializer.add("$T.captureCallState($S))", Linker.Option.class, "errno");
                }
                if (functionNode.useErrno()) {
                    methodBody.addStatement("var capturedState = offHeap.allocate(CAPTURED_STATE_LAYOUT)");
                    if (nativeReturnTypeMapping.carrierClass().equals(void.class)) {
                        methodBody.addStatement("$L.invoke($L)", nameMapper.get(functionNode), CodeBlock.join(arguments, ", "));
                        methodBody.addStatement("processError(capturedState, $S, $L)", methodsMap.get(functionNode), CodeBlock.join(arguments, ", "));
                    } else {
                        methodBody.addStatement("var callResult = ($T) $L.invoke($L)",
                                nativeReturnTypeMapping.carrierClass().equals(Object.class) ? MemorySegment.class : nativeReturnTypeMapping.carrierClass(),
                                nameMapper.get(functionNode), CodeBlock.join(arguments, ", "));
                        methodBody.addStatement("processError(callResult, capturedState, $S, $L)", methodsMap.get(functionNode), CodeBlock.join(arguments, ", "));
                    }
                } else {
                    if (nativeReturnTypeMapping.carrierClass().equals(void.class)) {
                        methodBody.addStatement("$L.invoke($L)", nameMapper.get(functionNode), CodeBlock.join(arguments, ", "));
                    } else {
                        methodBody.addStatement("var callResult = ($T) $L.invoke($L)",
                                nativeReturnTypeMapping.carrierClass().equals(Object.class) ? MemorySegment.class : nativeReturnTypeMapping.carrierClass(),
                                nameMapper.get(functionNode), CodeBlock.join(arguments, ", "));
                    }
                }

                var methodSpecBuilder = MethodSpec.overriding(methodsMap.get(functionNode)).addException(NativeMemoryException.class);
                if (functionNode.useErrno() || functionNode.functionParameters().stream().noneMatch(ParameterNode::returns)) {
                    methodSpecBuilder.beginControlFlow("try (var offHeap = $T.ofConfined())", Arena.class);
                } else {
                    methodSpecBuilder.beginControlFlow("try ");
                }
                methodBody.add(returnsCodeBlock.build());
                methodSpecBuilder.addCode(methodBody.build());
                methodSpecBuilder.nextControlFlow("catch ($T e)", Throwable.class)
                        .addStatement("throw new $T(e.getMessage(), e)", NativeMemoryException.class)
                        .endControlFlow();


                classBuilder.addField(FieldSpec.builder(MethodHandle.class, nameMapper.get(functionNode),
                                        Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                                .initializer(initializer.build())
                                .build())
                        .addMethod(methodSpecBuilder.build());
            }
        }

        var outputFile = JavaFile.builder(packageName, classBuilder.build()).indent("\t").skipJavaLangImports(true).build();
        return outputFile.toString();
    }
}

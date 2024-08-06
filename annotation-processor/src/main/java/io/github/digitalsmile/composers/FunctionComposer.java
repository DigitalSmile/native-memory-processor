package io.github.digitalsmile.composers;

import com.squareup.javapoet.*;
import io.github.digitalsmile.PackageName;
import io.github.digitalsmile.PrettyName;
import io.github.digitalsmile.annotation.NativeMemoryException;
import io.github.digitalsmile.annotation.function.NativeCall;
import io.github.digitalsmile.annotation.types.interfaces.OpaqueMemoryLayout;
import io.github.digitalsmile.functions.FunctionNode;
import io.github.digitalsmile.functions.ParameterNode;
import io.github.digitalsmile.headers.mapping.ArrayOriginalType;
import io.github.digitalsmile.headers.mapping.ObjectOriginalType;
import io.github.digitalsmile.headers.mapping.OriginalType;
import io.github.digitalsmile.headers.mapping.PrimitiveOriginalType;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeParameterElement;
import javax.tools.Diagnostic;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FunctionComposer {
    private final Messager messager;

    public FunctionComposer(Messager messager) {
        this.messager = messager;
    }

    public String compose(String packageName, String originalName, List<FunctionNode> nodes, Map<FunctionNode, String> nativeFunctionNames) {
        var context = ClassName.get(packageName, originalName + "Context");
        var classBuilder = TypeSpec.classBuilder(originalName + "Native")
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(ClassName.get(packageName, originalName))
                .superclass(NativeCall.class)
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addStatement("super(new $T())", context)
                        .build());

        for (FunctionNode functionNode : nodes) {
            var returnsCodeBlock = CodeBlock.builder();
            var options = functionNode.functionOptions();
            var returnType = functionNode.returnNode().getType();
            List<CodeBlock> arguments = new ArrayList<>();
            var methodBody = CodeBlock.builder();
            for (ParameterNode parameterNode : functionNode.functionParameters()) {
                var prettyName = PrettyName.getVariableName(parameterNode.name());
                var node = parameterNode.nativeMemoryNode();
                var parameterTypeMapping = node.getType();
                if (parameterNode.byAddress()) {
                    arguments.add(CodeBlock.builder().add("$LMemorySegment", prettyName).build());

                    switch (parameterTypeMapping) {
                        case ArrayOriginalType _ -> {
                            if (parameterTypeMapping.carrierClass().equals(String.class)) {
                                methodBody.addStatement("var $LMemorySegment = context.allocateFrom($T.JAVA_BYTE, $L)", prettyName,
                                        ValueLayout.class, prettyName);
                            } else {
                                methodBody.addStatement("var $LMemorySegment = context.allocateFrom($T.$L, $L)", prettyName,
                                        ValueLayout.class, parameterTypeMapping.valueLayoutName(), prettyName);
                            }
                        }
                        case ObjectOriginalType _ -> {
                            if (parameterTypeMapping.carrierClass().equals(String.class)) {
                                methodBody.addStatement("var $LMemorySegment = context.allocateFrom($L)", prettyName, prettyName);
                            } else if (node.getNodeType().isOpaque()) {
                                methodBody.addStatement("context.checkIsCreatedByArena($L.memorySegment())", prettyName);
                                methodBody.addStatement("var $LMemorySegment = $L.memorySegment()", prettyName, prettyName);
                            } else if (parameterTypeMapping.carrierClass().equals(OpaqueMemoryLayout.class)) {
                                methodBody.addStatement("var $LMemorySegment = $L.getMemorySegment()", prettyName, prettyName);
                            } else {
                                methodBody.addStatement("var $LMemorySegment = context.allocate($L.getMemoryLayout())", prettyName, prettyName);
                                methodBody.addStatement("$L.toBytes($LMemorySegment)", prettyName, prettyName);
                            }
                        }
                        case PrimitiveOriginalType _ -> {
                            methodBody.addStatement("var $LMemorySegment = context.allocate($T.$L)", prettyName, ValueLayout.class, parameterTypeMapping.valueLayoutName());
                            methodBody.addStatement("$LMemorySegment.set($T.$L, 0, $L)", prettyName, ValueLayout.class, parameterTypeMapping.valueLayoutName(), prettyName);
                        }
                        default -> throw new IllegalStateException("Unexpected value: " + parameterTypeMapping);
                    }
                } else if (node.getNodeType().isEnum()) {
                    arguments.add(CodeBlock.builder().add("$LValue", prettyName).build());
                    methodBody.addStatement("var $LValue = $L.getValue()", prettyName, prettyName);
                } else {
                    if (parameterTypeMapping instanceof ObjectOriginalType) {
                        arguments.add(CodeBlock.builder().add("$LMemorySegment", prettyName).build());
                    } else {
                        arguments.add(CodeBlock.builder().add("$L", prettyName).build());
                    }
                }

                if (parameterNode.returns()) {
                    switch (parameterTypeMapping) {
                        case ArrayOriginalType _ -> {
                            returnsCodeBlock.addStatement("return $LMemorySegment.toArray($T.$L)", prettyName, ValueLayout.class, parameterTypeMapping.valueLayoutName());
                        }
                        case ObjectOriginalType _ -> {
                            if (parameterTypeMapping.carrierClass().equals(String.class)) {
                                returnsCodeBlock.addStatement("return $LMemorySegment.equals($T.NULL) ? \"\" : $LMemorySegment.reinterpret($T.MAX_VALUE).getString(0)", prettyName, MemorySegment.class, Integer.class);
                            } else if (parameterTypeMapping.carrierClass().equals(Object.class)) {
                                returnsCodeBlock.addStatement("return $L.fromBytes($LMemorySegment)", prettyName, prettyName);
                            } else {
                                returnsCodeBlock.addStatement("return $L.createEmpty().fromBytes($LMemorySegment)", PrettyName.getObjectName(returnType.typeName()), prettyName);
                            }
                        }
                        case PrimitiveOriginalType _ -> {
                            returnsCodeBlock.addStatement("return $LMemorySegment.get($T.$L, 0)", prettyName, ValueLayout.class, parameterTypeMapping.valueLayoutName());
                        }
                        default -> throw new IllegalStateException("Unexpected value: " + parameterTypeMapping);
                    }
                }
            }

            if (options.useErrno()) {
                methodBody.addStatement("var capturedState = context.allocate(CAPTURED_STATE_LAYOUT)");
                methodBody.addStatement("var callResult = (int) $T.$L.invoke(capturedState, $L)", context, nativeFunctionNames.get(functionNode), CodeBlock.join(arguments, ", "));
                methodBody.addStatement("processError(callResult, capturedState, $S, $L)", functionNode.functionName(), CodeBlock.join(arguments, ", "));
            } else {
                if (returnType.carrierClass().equals(void.class)) {
                    methodBody.addStatement("$T.$L.invoke($L)", context, nativeFunctionNames.get(functionNode), CodeBlock.join(arguments, ", "));
                } else {
                    switch (returnType) {
                        case ArrayOriginalType _, ObjectOriginalType _ ->
                                methodBody.addStatement("var callResult = ($T) $T.$L.invoke($L)",
                                        functionNode.returnNode().getNodeType().isEnum() ? int.class : MemorySegment.class,
                                        context, nativeFunctionNames.get(functionNode), CodeBlock.join(arguments, ", "));
                        default -> methodBody.addStatement("var callResult = ($T) $T.$L.invoke($L)",
                                returnType.carrierClass(),
                                context, nativeFunctionNames.get(functionNode), CodeBlock.join(arguments, ", "));
                    }
                }
            }

            if (!returnType.carrierClass().equals(void.class) && functionNode.functionParameters().stream().noneMatch(ParameterNode::returns)) {
                if (returnType instanceof ObjectOriginalType) {
                    if (returnType.carrierClass().equals(String.class)) {
                        returnsCodeBlock.addStatement("return callResult.equals($T.NULL) ? \"\" : callResult.reinterpret($T.MAX_VALUE).getString(0)", MemorySegment.class, Integer.class);
                    } else {
                        var generatedPackageName = PackageName.getPackageName(returnType.typeName());
                        var prettyName = PrettyName.getObjectName(returnType.typeName());
                        var c = ClassName.get(generatedPackageName, prettyName);
                        returnsCodeBlock.addStatement("return $T.create(callResult)", c);
                    }
                } else {
                    returnsCodeBlock.addStatement("return callResult");
                }
            }

            var methodSpecBuilder = MethodSpec.methodBuilder(functionNode.functionName())
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(AnnotationSpec.builder(Override.class).build())
                    .returns(createTypeName(returnType))
                    .addException(NativeMemoryException.class);

            var returnTypeName = functionNode.returnNode().getType().typeName();
            for (TypeParameterElement typeParameterElement : functionNode.typeVariables()) {
                methodSpecBuilder.addTypeVariable(TypeVariableName.get(typeParameterElement));
                if (returnTypeName.equals(typeParameterElement.asType().toString())) {
                    methodSpecBuilder.returns(TypeName.get(typeParameterElement.asType()));
                }
            }

            for (ParameterNode parameterNode : functionNode.functionParameters()) {
                var typeParameter = methodSpecBuilder.typeVariables.stream().filter(p -> p.name.equals(parameterNode.nativeMemoryNode().getType().typeName())).findFirst().orElse(null);
                var typeName = typeParameter == null ? createTypeName(parameterNode.nativeMemoryNode().getType()) : typeParameter;
                methodSpecBuilder.addParameter(ParameterSpec.builder(typeName, parameterNode.name()).build());
            }

            methodSpecBuilder.beginControlFlow("try");
            methodBody.add(returnsCodeBlock.build());
            methodSpecBuilder.addCode(methodBody.build());
            methodSpecBuilder.nextControlFlow("catch ($T e)", Throwable.class)
                    .addStatement("throw new $T(e.getMessage(), e)", NativeMemoryException.class)
                    .endControlFlow();
            classBuilder.addMethod(methodSpecBuilder.build());
        }

        var builder = JavaFile.builder(packageName, classBuilder.build());
        var outputFile = builder.indent("\t").skipJavaLangImports(true).build();
        return outputFile.toString();
    }

    private TypeName createTypeName(OriginalType type) {
        switch (type) {
            case ArrayOriginalType arrayOriginalType -> {
                var parameterPackageName = PackageName.getPackageName(type.typeName());
                if (arrayOriginalType.isObjectType()) {
                    return ClassName.get(parameterPackageName, PrettyName.getObjectName(type.typeName()) + "[]");
                } else {
                    return TypeName.get(type.carrierClass().arrayType());
                }
            }
            case ObjectOriginalType _ -> {
                if (type.carrierClass().equals(Object.class)) {
                    var parameterPackageName = PackageName.getPackageName(type.typeName());
                    return ClassName.get(parameterPackageName, PrettyName.getObjectName(type.typeName()));
                } else {
                    return TypeName.get(type.carrierClass());
                }
            }
            case PrimitiveOriginalType _ -> {
                return TypeName.get(type.carrierClass());
            }
            default -> throw new IllegalStateException("Unexpected value: " + type);
        }
    }
}

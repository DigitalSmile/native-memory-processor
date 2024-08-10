package io.github.digitalsmile.composers;

import com.squareup.javapoet.*;
import io.github.digitalsmile.PackageName;
import io.github.digitalsmile.PrettyName;
import io.github.digitalsmile.annotation.function.NativeCall;
import io.github.digitalsmile.functions.FunctionNode;
import io.github.digitalsmile.functions.ParameterNode;
import io.github.digitalsmile.headers.mapping.*;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Modifier;
import javax.tools.Diagnostic;
import java.lang.foreign.MemorySegment;
import java.util.List;
import java.util.Map;

public class AutoFunctionComposer {
    private final Messager messager;

    public AutoFunctionComposer(Messager messager) {
        this.messager = messager;
    }

    public String compose(String packageName, String originalName, List<FunctionNode> nodes) {
        var classBuilder = TypeSpec.interfaceBuilder(originalName + "AutoFunctions")
                //.addModifiers(Modifier.PUBLIC)
                .addSuperinterface(ClassName.get(packageName, originalName));

        for (FunctionNode node : nodes) {
            var method = MethodSpec.methodBuilder(PrettyName.getVariableName(node.functionName()));

            var returnType = node.returnNode().getType();
            switch (returnType) {
                case ArrayOriginalType arrayOriginalType -> {
                    method.returns(createType(arrayOriginalType));
//                        if (arrayOriginalType.carrierClass().equals(String.class)) {
//                            method.returns(String.class);
//                        } else if (arrayOriginalType.carrierClass().equals(Object.class)) {
//                            method.returns(Object.class.arrayType());
//                        } else {
//                            var typePackageName = PackageName.getPackageName(arrayOriginalType.typeName());
//                            method.returns(ClassName.get(typePackageName, PrettyName.getObjectName(arrayOriginalType.typeName())));
//                        }
                }
                case ObjectOriginalType objectOriginalType -> {
                    method.returns(createType(objectOriginalType));
                   // messager.printMessage(Diagnostic.Kind.ERROR, objectOriginalType.typeName() + " " + objectOriginalType.carrierClass());
//                    if (objectOriginalType.carrierClass().equals(void.class)) {
//                     method.returns(void.class);
//                    } else {
//                        var typePackageName = PackageName.getPackageName(objectOriginalType.typeName());
//                        method.returns(ClassName.get(typePackageName, PrettyName.getObjectName(objectOriginalType.typeName())));
//                    }
                }
                case PrimitiveOriginalType _ -> {
                    method.returns(returnType.carrierClass());
                }
                default -> {
                }
            }


            //messager.printMessage(Diagnostic.Kind.ERROR, "F: " + node.functionName());
            for (ParameterNode parameterNode : node.functionParameters()) {
                //messager.printMessage(Diagnostic.Kind.ERROR, node.functionName() + " " + parameterNode.nativeMemoryNode().getName());
                var name = parameterNode.nativeMemoryNode().getName();
                if (name.isEmpty()) {
                    name = "arg" + node.functionParameters().indexOf(parameterNode);
                }
                var parameterType = parameterNode.nativeMemoryNode().getType();
                var parameterName = PrettyName.getVariableName(name);
                switch (parameterType) {
                    case ArrayOriginalType arrayOriginalType -> {
                        method.addParameter(createType(arrayOriginalType), name);
//                        if (arrayOriginalType.isObjectType()) {
//                            if (arrayOriginalType.carrierClass().equals(String.class)) {
//                                method.addParameter(String.class, name);
//                            } else if (arrayOriginalType.carrierClass().equals(Object.class)) {
//                                method.addParameter(Object.class.arrayType(), name);
//                            } else {
//                                var typePackageName = PackageName.getPackageName(arrayOriginalType.typeName());
//                                method.addParameter(ClassName.get(typePackageName, PrettyName.getObjectName(arrayOriginalType.typeName())), name);
//                            }
//                        } else {
//                            method.addParameter(arrayOriginalType.carrierClass().arrayType(), name);
//                        }
                    }
                    case ObjectOriginalType objectOriginalType -> {
                        method.addParameter(createType(objectOriginalType), name);
//                        if (objectOriginalType.carrierClass().equals(void.class)) {
//                            method.addParameter(MemorySegment.class, name);
//                        } else {
//                            var typePackageName = PackageName.getPackageName(objectOriginalType.typeName());
//                            method.addParameter(ClassName.get(typePackageName, PrettyName.getObjectName(objectOriginalType.typeName())), parameterName);
//                        }
                    }
                    case PrimitiveOriginalType _ -> {
                        method.addParameter(parameterType.carrierClass(), parameterName);
                    }
                    default -> {
                    }
                }

            }

            classBuilder.addMethod(method
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT).build());
        }


        var builder = JavaFile.builder(packageName, classBuilder.build());
        var outputFile = builder.indent("\t").skipJavaLangImports(true).build();
        return outputFile.toString();
    }


    private TypeName createType(OriginalType type, boolean isArray) {
        var typeName = type.typeName();
        var clazz = type.carrierClass();
        if (type.carrierClass().equals(Object.class)) {
            var resolvedPackageName = PackageName.getPackageName(typeName);
            if (typeName.equals(String.class.getSimpleName())) {
                resolvedPackageName = "";
            }
            return ClassName.get(resolvedPackageName, PrettyName.getObjectName(typeName) + (isArray ? "[]" : ""));
        } else {
            return TypeName.get(isArray ? clazz.arrayType() : clazz);
        }
    }

    private TypeName createType(OriginalType type) {
        return createType(type, false);
    }
}

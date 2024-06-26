package io.github.digitalsmile.composers;

import com.squareup.javapoet.*;
import io.github.digitalsmile.NativeProcessor;
import io.github.digitalsmile.annotation.function.*;
import io.github.digitalsmile.type.ObjectTypeMapping;
import io.github.digitalsmile.type.TypeMapping;
import io.github.digitalsmile.annotation.structure.NativeMemoryLayout;

import javax.annotation.processing.Messager;
import javax.lang.model.element.*;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.github.digitalsmile.composers.StructComposer.PRIMITIVE_MAPPING;

public class FunctionComposer {

    public static String compose(String packageName, String javaName, List<Element> elements, Map<NativeProcessor.Library, Set<String>> libraries, Messager messager, List<TypeVariableName> processedTypeNames) {
        var classBuilder = TypeSpec.classBuilder(javaName + "Native")
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(ClassName.get(packageName, javaName))
                .superclass(NativeFunction.class);

        var nameAllocator = new NameAllocator();

        for (Map.Entry<NativeProcessor.Library, Set<String>> libraryEntry : libraries.entrySet()) {
            var library = libraryEntry.getKey();
            if (library.libraryName().equals("libc")) {
                continue;
            }
            var initializer = CodeBlock.builder();
            if (library.isAlreadyLoaded()) {
                initializer.add("$T.loaderLookup()", SymbolLookup.class);
            } else if (library.libraryName().startsWith("/")) {
                initializer.add("$T.libraryLookup(Path.of($S), Arena.global())", SymbolLookup.class, library.libraryName());
            } else {
                initializer.add("$T.libraryLookup($S, Arena.global())", SymbolLookup.class, library.libraryName());
            }
            classBuilder.addField(FieldSpec.builder(SymbolLookup.class,
                            library.libraryName().toUpperCase() + "_LIB",
                            Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .initializer(initializer.build())
                    .build());
        }
        for (Element element : elements) {
            var method = (ExecutableElement) element;
            var functionAnnotation = element.getAnnotation(Function.class);
            var uniqueName = nameAllocator.newName(functionAnnotation.name().toUpperCase() + elements.indexOf(element));

            // hack: https://stackoverflow.com/questions/54962817/how-to-get-annotation-parameter-in-annotation-processor
            TypeMirror returnTypeMirror = null;
            try {
                var exc = functionAnnotation.returnType();
            } catch (MirroredTypeException mte) {
                returnTypeMirror = mte.getTypeMirror();
            }

            var nativeReturnType = findType(returnTypeMirror, processedTypeNames);
            if (nativeReturnType == null) {
                nativeReturnType = findType(method.getReturnType(), processedTypeNames);
            }
            var methodReturnType = findType(method.getReturnType(), processedTypeNames);

            List<CodeBlock> parameters = new ArrayList<>();
            if (!nativeReturnType.carrierClass().equals(void.class)) {
                parameters.add(CodeBlock.builder().add("$T.$L", ValueLayout.class, nativeReturnType.valueLayoutName()).build());
            }

            List<CodeBlock> arguments = new ArrayList<>();
            VariableElement returnElement = null;
            List<VariableElement> byAddressElements = new ArrayList<>();
            for (VariableElement variableElement : method.getParameters()) {
                var byAddressAnnotation = variableElement.getAnnotation(ByAddress.class);
                var returnsAnnotation = variableElement.getAnnotation(Returns.class);
                var variableType = findType(variableElement.asType(), processedTypeNames);
                if (byAddressAnnotation != null) {
                    parameters.add(CodeBlock.builder().add("$T.ADDRESS", ValueLayout.class).build());
                    byAddressElements.add(variableElement);
                    arguments.add(CodeBlock.builder().add("$LMemorySegment", variableElement.getSimpleName()).build());
                } else {
                    parameters.add(CodeBlock.builder().add("$T.$L", ValueLayout.class, variableType.valueLayoutName()).build());
                    if (variableType instanceof ObjectTypeMapping) {
                        arguments.add(CodeBlock.builder().add("$LMemorySegment", variableElement.getSimpleName()).build());
                        byAddressElements.add(variableElement);
                    } else {
                        arguments.add(CodeBlock.builder().add("$L", variableElement.getSimpleName()).build());
                    }
                }
                if (returnsAnnotation != null) {
                    if (returnElement != null) {
                        messager.printMessage(Diagnostic.Kind.ERROR, "Only one @Returns annotation is allowed", variableElement);
                    }
                    if (!methodReturnType.carrierClass().equals(variableType.carrierClass())) {
                        messager.printMessage(Diagnostic.Kind.ERROR, "Parameter with @Returns annotation should be the same type with method call", variableElement);
                    }
                    if (variableType instanceof ObjectTypeMapping) {
                        var generatedTypes = processedTypeNames.stream().filter(type -> type.name.equals(variableElement.asType().toString())).findFirst().orElse(null);
                        if (generatedTypes == null) {
                            var methodRealTypes = method.getTypeParameters().stream().findFirst().orElse(null);
                            if (methodRealTypes == null) {
                                messager.printMessage(Diagnostic.Kind.ERROR, "Parameter with object definition should extend NativeMemoryLayout interface", variableElement);
                            } else {
                                var boundsType = methodRealTypes.getBounds().stream().findFirst().orElse(null);
                                if (boundsType == null || !boundsType.toString().equals(NativeMemoryLayout.class.getName())) {
                                    messager.printMessage(Diagnostic.Kind.ERROR, "Parameter with object definition should extend NativeMemoryLayout interface", variableElement);
                                } else {
                                    returnElement = variableElement;
                                }
                            }
                        } else {
                            returnElement = variableElement;
                        }
                    } else {
                        returnElement = variableElement;
                    }
                }
            }

            if (returnElement == null && !nativeReturnType.carrierClass().equals(methodReturnType.carrierClass())) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Return type should of native and determined function should be equal, unless @Returns annotation defined", method);
            }

            CodeBlock.Builder initializer = CodeBlock.builder().add("$T.nativeLinker().downcallHandle($W", Linker.class);
            var library = libraries.entrySet().stream().filter(e -> e.getKey().libraryName().equals(functionAnnotation.library())).findFirst().orElseThrow();
            if (library.getKey().libraryName().equals("libc")) {
                initializer.add("STD_LIB.find($S).orElseThrow(),$W", functionAnnotation.name());
            } else {
                initializer.add("$L.find($S).orElseThrow(),$W", library.getKey().libraryName().toUpperCase() + "_LIB", functionAnnotation.name());
            }

            initializer.add("$T.$L($L)$L", FunctionDescriptor.class,
                    nativeReturnType.carrierClass().equals(void.class) ? "ofVoid" : "of",
                    CodeBlock.join(parameters, ", "), functionAnnotation.useErrno() ? "," : ")");


            if (functionAnnotation.useErrno()) {
                initializer.add("$W");
                initializer.add("$T.captureCallState($S))", Linker.Option.class, "errno");
            }

            var methodBody = CodeBlock.builder();
            for (VariableElement variableElement : byAddressElements) {
                var elementType = findType(variableElement.asType(), processedTypeNames);
                if (elementType.carrierClass().equals(NativeMemoryLayout.class) || elementType.carrierClass().equals(MemorySegment.class)) {
                    methodBody.addStatement("var $LMemorySegment = offHeap.allocate($L.getMemoryLayout())", variableElement.getSimpleName(), variableElement.getSimpleName());
                    methodBody.addStatement("$L.toBytes($LMemorySegment)", variableElement.getSimpleName(), variableElement.getSimpleName());
                } else {
                    methodBody.addStatement("var $LMemorySegment = offHeap.allocate($T.$L)", variableElement.getSimpleName(), ValueLayout.class, elementType.valueLayoutName());
                    methodBody.addStatement("$LMemorySegment.set($T.$L, 0, $L)", variableElement.getSimpleName(), ValueLayout.class, elementType.valueLayoutName(), variableElement.getSimpleName());
                }
            }

            if (functionAnnotation.useErrno()) {
                methodBody.addStatement("var capturedState = offHeap.allocate(CAPTURED_STATE_LAYOUT)");
                var capturedState = CodeBlock.builder().add("capturedState").build();
                arguments.addFirst(capturedState);
                if (nativeReturnType.carrierClass().equals(void.class)) {
                    methodBody.addStatement("$L.invoke($L)", uniqueName, CodeBlock.join(arguments, ", "));
                    methodBody.addStatement("processError(capturedState, $S, $L)", method.toString(), CodeBlock.join(arguments, ", "));
                } else {
                    methodBody.addStatement("var callResult = ($T) $L.invoke($L)",
                            nativeReturnType.carrierClass().equals(NativeMemoryLayout.class) ? MemorySegment.class : nativeReturnType.carrierClass(),
                            uniqueName, CodeBlock.join(arguments, ", "));
                    methodBody.addStatement("processError(callResult, capturedState, $S, $L)", method.toString(), CodeBlock.join(arguments, ", "));
                }
            } else {
                if (nativeReturnType.carrierClass().equals(void.class)) {
                    methodBody.addStatement("$L.invoke($L)", uniqueName, CodeBlock.join(arguments, ", "));
                } else {
                    methodBody.addStatement("var callResult = ($T) $L.invoke($L)",
                            nativeReturnType.carrierClass().equals(NativeMemoryLayout.class) ? MemorySegment.class : nativeReturnType.carrierClass(),
                            uniqueName, CodeBlock.join(arguments, ", "));
                }
            }
            if (returnElement != null) {
                var elementType = findType(returnElement.asType(), processedTypeNames);
                if (elementType.carrierClass().equals(NativeMemoryLayout.class)) {
                    if (nativeReturnType.carrierClass().equals(NativeMemoryLayout.class)) {
                        var t = returnTypeMirror.toString().replace("<any?>.", "");
                        var processedType = processedTypeNames.stream().filter(processed -> processed.name.equals(t)).findFirst().orElse(null);
                        methodBody.addStatement("return $L.createEmpty().fromBytes(callResult)", processedType.name);
                    } else {
                        methodBody.addStatement("return $L.fromBytes($LMemorySegment)", returnElement.getSimpleName(), returnElement.getSimpleName());
                    }
                } else {
                    methodBody.addStatement("return $LMemorySegment.get($T.$L, 0)", returnElement.getSimpleName(), ValueLayout.class, methodReturnType.valueLayoutName());
                }
            } else {
                if (!methodReturnType.carrierClass().equals(void.class)) {
                    methodBody.addStatement("return callResult");
                }
            }


//            if (functionAnnotation.useErrno()) {
//                if (!methodReturnType.carrierClass().equals(void.class)) {
//                    methodBody.addStatement("var capturedState = offHeap.allocate(CAPTURED_STATE_LAYOUT)");
//                    methodBody.addStatement("var callResult = ($T) $L.invoke(capturedState, $L)",
//                            nativeReturnType.carrierClass().equals(NativeMemoryLayout.class) ? MemorySegment.class : nativeReturnType.carrierClass(),
//                            uniqueName, CodeBlock.join(arguments, ", "));
//                    methodBody.addStatement("processError(callResult, capturedState, $S, $L)", method.toString(), CodeBlock.join(arguments, ", "));
//                    if (returnElement != null) {
//                        var elementType = findType(returnElement.asType(), processedTypeNames);
//                        if (elementType.carrierClass().equals(NativeMemoryLayout.class)) {
//                            if (nativeReturnType.carrierClass().equals(NativeMemoryLayout.class)) {
//                                var t = returnTypeMirror.toString().replace("<any?>.", "");
//                                var processedType = processedTypeNames.stream().filter(processed -> processed.name.equals(t)).findFirst().orElse(null);
//                                methodBody.addStatement("return $L.createEmpty().fromBytes(callResult.get($T.ADDRESS, 0))", processedType.name, ValueLayout.class);
//                            } else {
//                                methodBody.addStatement("return $L.fromBytes($LMemorySegment)", returnElement.getSimpleName(), returnElement.getSimpleName());
//                            }
//                        } else {
//                            methodBody.addStatement("return $LMemorySegment.get($T.$L, 0)", returnElement.getSimpleName(), ValueLayout.class, methodReturnType.valueLayoutName());
//                        }
//                    } else {
//                        methodBody.addStatement("return callResult");
//                    }
//                } else {
//                    methodBody.addStatement("var capturedState = offHeap.allocate(CAPTURED_STATE_LAYOUT)");
//                    methodBody.addStatement("$L.invoke($L)", uniqueName, CodeBlock.join(arguments, ", "));
//                    methodBody.addStatement("processError(capturedState, $S, $L)", method.toString(), CodeBlock.join(arguments, ", "));
//                }
//            } else {
//                if (!methodReturnType.carrierClass().equals(void.class)) {
//                    if (returnElement != null) {
//                        var elementType = findType(returnElement.asType(), processedTypeNames);
//                        methodBody.addStatement("var callResult = ($T) $L.invoke($L)",
//                                nativeReturnType.carrierClass().equals(NativeMemoryLayout.class) ? MemorySegment.class : nativeReturnType.carrierClass(),
//                                uniqueName, CodeBlock.join(arguments, ", "));
//                        if (elementType.carrierClass().equals(NativeMemoryLayout.class)) {
//                            if (nativeReturnType.carrierClass().equals(NativeMemoryLayout.class)) {
//                                var t = returnTypeMirror.toString().replace("<any?>.", "");
//                                var processedType = processedTypeNames.stream().filter(processed -> processed.name.equals(t)).findFirst().orElse(null);
//                                methodBody.addStatement("return $L.createEmpty().fromBytes(callResult.get($T.ADDRESS, 0))", processedType.name, ValueLayout.class);
//                            } else {
//                                methodBody.addStatement("return $L.fromBytes($LMemorySegment)", returnElement.getSimpleName(), returnElement.getSimpleName());
//                            }
//                        } else {
//                            methodBody.addStatement("return $LMemorySegment.get($T.$L, 0)", returnElement.getSimpleName(), ValueLayout.class, methodReturnType.valueLayoutName());
//                        }
//                    } else {
//                        methodBody.addStatement("return ($T) $L.invoke($L)", nativeReturnType.carrierClass(),
//                                uniqueName, CodeBlock.join(arguments, ", "));
//                    }
//                } else {
//                    methodBody.addStatement("$L.invoke($L)", uniqueName, CodeBlock.join(arguments, ", "));
//                }
//            }

            var methodSpecBuilder = MethodSpec.overriding(method).addException(NativeMemoryException.class);
            if (functionAnnotation.useErrno() || !byAddressElements.isEmpty()) {
                methodSpecBuilder.beginControlFlow("try (var offHeap = $T.ofConfined())", Arena.class);
            } else {
                methodSpecBuilder.beginControlFlow("try ");
            }
            methodSpecBuilder.addCode(methodBody.build());
            methodSpecBuilder.nextControlFlow("catch ($T e)", Throwable.class)
                    .addStatement("throw new $T(e.getMessage(), e)", NativeMemoryException.class)
                    .endControlFlow();


            classBuilder.addField(FieldSpec.builder(MethodHandle.class, uniqueName,
                                    Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                            .initializer(initializer.build())
                            .build())
                    .addMethod(methodSpecBuilder.build());
        }

        var outputFile = JavaFile.builder(packageName, classBuilder.build()).indent("\t").skipJavaLangImports(true).build();
        return outputFile.toString();
    }

    private static TypeMapping findType(TypeMirror type, List<TypeVariableName> processedTypeNames) {
        var foundType = PRIMITIVE_MAPPING.stream().filter(list -> {
            return list.carrierClass().getSimpleName().equals(type.toString());
        }).findFirst().orElse(null);
        if (foundType == null) {
            if (!type.toString().equals(void.class.getName())) {
                foundType = new ObjectTypeMapping(NativeMemoryLayout.class);
            }
        }
        return foundType;
    }
}

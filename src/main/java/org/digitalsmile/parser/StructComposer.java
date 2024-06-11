package org.digitalsmile.parser;

import com.squareup.javapoet.*;
import org.digitalsmile.NativeMemoryLayout;
import org.openjdk.jextract.Type;

import javax.lang.model.element.Modifier;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.Map.entry;
import static org.openjdk.jextract.Type.Primitive.Kind.*;

public class StructComposer {

    private static final List<TypeMapping> typeMapping = List.of(
            new TypeMapping(Int, ValueLayout.JAVA_INT),
            new TypeMapping(List.of(Char, Char16), ValueLayout.JAVA_BYTE),
            new TypeMapping(List.of(Long, LongLong), ValueLayout.JAVA_LONG),
            new TypeMapping(Short, ValueLayout.JAVA_SHORT),
            new TypeMapping(Float, ValueLayout.JAVA_FLOAT),
            new TypeMapping(Double, ValueLayout.JAVA_DOUBLE)
    );


//    private static final Map<String, String> MEMORY_LAYOUT_VALUES_MAPPING = Map.of(
//            Int.toString(), "JAVA_INT",
//            Char.toString(), "JAVA_BYTE",
//            Type.Primitive.Kind.Long.toString(), "JAVA_LONG",
//            Type.Primitive.Kind.LongLong.toString(), "JAVA_LONG",
//            Type.Primitive.Kind.Short.toString(), "JAVA_SHORT",
//            Type.Primitive.Kind.Float.toString(), "JAVA_FLOAT",
//            Type.Primitive.Kind.Double.toString(), "JAVA_DOUBLE"
//    );
//
//    private static final Map<String, Class<?>> TYPE_TO_ARRAY_MAPPING = Map.of(
//            Int.toString(), int.class.arrayType(),
//            Char.toString(), byte.class.arrayType(),
//            Type.Primitive.Kind.Long.toString(), long.class.arrayType(),
//            Type.Primitive.Kind.LongLong.toString(), long.class.arrayType(),
//            Type.Primitive.Kind.Short.toString(), short.class.arrayType(),
//            Type.Primitive.Kind.Float.toString(), float.class.arrayType(),
//            Type.Primitive.Kind.Double.toString(), double.class.arrayType()
//    );
//    private static final Map<String, Class<?>> TYPE_MAPPING = Map.of(
//            Int.toString(), int.class,
//            Char.toString(), byte.class,
//            Type.Primitive.Kind.Long.toString(), long.class,
//            Type.Primitive.Kind.LongLong.toString(), long.class,
//            Type.Primitive.Kind.Short.toString(), short.class,
//            Type.Primitive.Kind.Float.toString(), float.class,
//            Type.Primitive.Kind.Double.toString(), double.class
//    );
//
//    private static final Map<Class<?>, String> EMPTY_VALUES_MAPPING = Map.ofEntries(
//            entry(byte[].class, "new byte[]{}"),
//            entry(int.class, "0"),
//            entry(int[].class, "new int[]{}"),
//            entry(long.class, "0"),
//            entry(long[].class, "new long[]{}"),
//            entry(short.class, "(short) 0"),
//            entry(short[].class, "new short[]{}"),
//            entry(float.class, "0"),
//            entry(float[].class, "new float[]{}"),
//            entry(double.class, "0"),
//            entry(double[].class, "new double[]{}")
//    );
//
//    private static final Map<Class<?>, String> TYPE_EMPTY_MAPPING = Map.ofEntries(
//            entry(byte.class, " > 0"),
//            entry(byte[].class, ".length > 0"),
//            entry(int.class, " > 0"),
//            entry(int[].class, ".length > 0"),
//            entry(long.class, " > 0"),
//            entry(long[].class, ".length > 0"),
//            entry(short.class, " > 0"),
//            entry(short[].class, ".length > 0"),
//            entry(float.class, " > 0"),
//            entry(float[].class, ".length > 0"),
//            entry(double.class, " > 0"),
//            entry(double[].class, ".length > 0"),
//            entry(NativeMemoryLayout.class, ".isEmpty()"),
//            entry(NativeMemoryLayout[].class, ".length > 0")
//    );


    public static String compose(NativeMemoryModel nativeMemoryModel, String packageName, String prettyName, Function<String, String> lookupCallback) {
        List<CodeBlock> memoryLayout = processMemoryLayout(nativeMemoryModel.getNodes(), lookupCallback);
        List<FieldSpec> constructorFields = processConstructorParameters(nativeMemoryModel.getNodes(), lookupCallback);
        List<FieldSpec> handlesFields = processHandleFields(nativeMemoryModel.getNodes(), null, lookupCallback);
        List<CodeBlock> emptyConstructorStatements = processEmptyConstructor(nativeMemoryModel.getNodes(), lookupCallback);
        List<CodeBlock> fromBytesBodyStatements = processFromBodyStatements(nativeMemoryModel.getNodes(), lookupCallback);
        List<CodeBlock> fromByesBodyStatements = processFromReturnStatements(nativeMemoryModel.getNodes(), false, lookupCallback);
        List<CodeBlock> toByesBodyStatements = processToBodyStatements(nativeMemoryModel.getNodes(), false, lookupCallback);
        List<CodeBlock> isEmptyReturnStatements = processIsEmptyReturnStatements(nativeMemoryModel.getNodes());

        var outputFile = JavaFile.builder(packageName,
                TypeSpec.recordBuilder(prettyName)
                        .addModifiers(Modifier.PUBLIC)
                        .addSuperinterface(TypeName.get(NativeMemoryLayout.class))
                        .addFields(constructorFields)
                        .addField(FieldSpec.builder(TypeName.get(MemoryLayout.class), "LAYOUT", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                                .initializer(
                                        CodeBlock.builder()
                                                .add("$T.structLayout(\n\t", MemoryLayout.class)
                                                .add(CodeBlock.join(memoryLayout, ",\n\t"))
                                                .add("\n)")
                                                .build())
                                .build())
                        .addFields(handlesFields)
                        .addMethod(MethodSpec.methodBuilder("createEmpty")
                                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                .returns(ClassName.get(packageName, prettyName))
                                .addStatement("return new $N($L)", prettyName, CodeBlock.join(emptyConstructorStatements, ", ")).build())
                        .addMethod(MethodSpec.methodBuilder("fromBytes")
                                .addModifiers(Modifier.PUBLIC)
                                .addAnnotation(AnnotationSpec.builder(Override.class).build())
                                .addAnnotation(AnnotationSpec.builder(SuppressWarnings.class).addMember("value", "$S", "unchecked").build())
                                .addParameter(TypeName.get(MemorySegment.class), "buffer")
                                .addException(TypeName.get(Throwable.class))
                                .returns(ClassName.get(packageName, prettyName))
                                .addCode(CodeBlock.join(fromBytesBodyStatements, ""))
                                .addCode("return new $L(\n\t", ClassName.get(packageName, prettyName).simpleName())
                                .addCode(CodeBlock.join(fromByesBodyStatements, ",\n\t"))
                                .addCode(");")
                                .build())
                        .addMethod(MethodSpec.methodBuilder("toBytes")
                                .addModifiers(Modifier.PUBLIC)
                                .addAnnotation(AnnotationSpec.builder(Override.class).build())
                                .addParameter(TypeName.get(MemorySegment.class), "buffer")
                                .addException(TypeName.get(Throwable.class))
                                .returns(TypeName.VOID)
                                .addCode(CodeBlock.join(toByesBodyStatements, ""))
                                .build())
                        .addMethod(MethodSpec.methodBuilder("isEmpty")
                                .addModifiers(Modifier.PUBLIC)
                                .addAnnotation(AnnotationSpec.builder(Override.class).build())
                                .returns(TypeName.BOOLEAN.unbox())
                                .addCode(CodeBlock.builder().addStatement("return $L", CodeBlock.join(isEmptyReturnStatements, " && ")).build())
                                .build())
                        .build()
        ).indent("\t").skipJavaLangImports(true).build();
        return outputFile.toString();
    }

    private static TypeMapping findType(Type.Primitive.Kind type) {
        return typeMapping.stream().filter(list -> list.types().contains(type)).findFirst().orElseThrow();
    }

    private static List<CodeBlock> processMemoryLayout(List<NativeMemoryNode> nodes, Function<String, String> lookupCallback) {
        List<CodeBlock> memoryLayouts = new ArrayList<>();
        for (NativeMemoryNode node : nodes) {
            var type = node.getType();
            if (type == null) {
                memoryLayouts.add(
                        CodeBlock.builder()
                                .add("$T.unionLayout(\n\t\t", MemoryLayout.class)
                                .add(CodeBlock.join(processMemoryLayout(node.getNodes(), lookupCallback), ",\n\t\t"))
                                .add("\n\t).withName($S)", node.getName())
                                .build());
            } else {
                switch (type) {
                    case Type.Array typeArray -> {
                        if (typeArray.elementType() instanceof Type.Primitive typePrimitive) {
                            var valueLayout = findType(typePrimitive.kind());
                            memoryLayouts.add(
                                    CodeBlock.builder().add("$T.sequenceLayout($L, $T.$L).withName($S)", MemoryLayout.class,
                                                    typeArray.elementCount().orElseThrow(), ValueLayout.class, valueLayout.valueLayoutName(), node.getName())
                                            .build());
                        } else if (typeArray.elementType() instanceof Type.Declared typeDeclared) {
                            var prettyName = lookupCallback.apply(typeDeclared.tree().name());
                            if (prettyName != null) {
                                memoryLayouts.add(CodeBlock.builder().add("$T.sequenceLayout($L, $L.LAYOUT).withName($S)", MemoryLayout.class,
                                                typeArray.elementCount().orElseThrow(), prettyName, node.getName())
                                        .build());
                            } else {
                                System.err.println("Struct '" + node.getName() + "' is not present in parsing queue. Please, explicitly add struct in annotation.");
                            }
                        } else {
                            System.err.println("'" + node.getName() + "': unsupported nested arrays with non-primitive types ('" + typeArray + "')");
                        }
                    }
                    case Type.Primitive typePrimitive -> {
                        var valueLayout = findType(typePrimitive.kind());
                        memoryLayouts.add(CodeBlock.builder().add("$T.$L.withName($S)", ValueLayout.class, valueLayout.valueLayoutName(), node.getName()).build());
                    }
                    case Type.Declared typeDeclared -> {
                        var prettyName = lookupCallback.apply(typeDeclared.tree().name());
                        if (prettyName != null) {
                            memoryLayouts.add(CodeBlock.builder().add("$L.LAYOUT.withName($S)", prettyName, node.getName()).build());
                        } else {
                            System.err.println("Struct '" + node.getName() + "' is not present in parsing queue. Please, explicitly add struct in annotation.");
                        }
                    }
                    default -> System.err.println("Unknown type '" + type + "' for field '" + node.getName() + "'");
                }
//
//
//                CodeBlock layoutCodeBlock;
//                if (type instanceof Type.Declared typeDeclared) {
//                    var prettyName = lookupCallback.apply(typeDeclared.tree().name());
//                    if (prettyName != null) {
//                        layoutCodeBlock = CodeBlock.builder().add("$L.$L", prettyName, "LAYOUT").build();
//                    } else {
//                        System.err.println("Struct '" + node.getName() + "' is not present in parsing queue. Please, explicitly add struct in annotation.");
//                        continue;
//                    }
//                } else if (type instanceof Type.Primitive typePrimitive) {
//                    layoutCodeBlock = CodeBlock.builder().add("$T.$L", ValueLayout.class, findType(typePrimitive.kind()).valueLayoutName()).build();
//                    //layoutCodeBlock = CodeBlock.builder().add("$T.$L", ValueLayout.class, MEMORY_LAYOUT_VALUES_MAPPING.get(type.toString())).build();
//                } else {
//                    System.err.println("Unknown type '" + type + "' for field '" + node.getName() + "'");
//                    continue;
//                }
//                if (node.isArray()) {
//                    memoryLayouts.add(
//                            CodeBlock.builder()
//                                    .add("$T.sequenceLayout($L, $L).withName($S)", MemoryLayout.class,
//                                            node.getArraySize(), layoutCodeBlock, node.getName())
//                                    .build());
//                } else {
//                    memoryLayouts.add(
//                            CodeBlock.builder()
//                                    .add("$L.withName($S)", layoutCodeBlock, node.getName())
//                                    .build());
//                }
            }
        }
        return memoryLayouts;
    }

    private static List<FieldSpec> processConstructorParameters(List<NativeMemoryNode> nodes, Function<String, String> lookupCallback) {
        List<FieldSpec> fieldSpecs = new ArrayList<>();
        for (NativeMemoryNode node : nodes) {
            var type = node.getType();
            if (type == null) {
                fieldSpecs.addAll(processConstructorParameters(node.getNodes(), lookupCallback));
            } else {
                switch (type) {
                    case Type.Array typeArray -> {
                        if (typeArray.elementType() instanceof Type.Primitive typePrimitive) {
                            var valueType = findType(typePrimitive.kind());
                            fieldSpecs.add(FieldSpec.builder(valueType.arrayClass(), node.getPrettyName()).build());
                        } else if (typeArray.elementType() instanceof Type.Declared typeDeclared) {
                            var prettyName = lookupCallback.apply(typeDeclared.tree().name());
                            if (prettyName != null) {
                                fieldSpecs.add(FieldSpec.builder(ClassName.get("", prettyName + "[]"), node.getPrettyName()).build());
                            } else {
                                System.err.println("Struct '" + node.getName() + "' is not present in parsing queue. Please, explicitly add struct in annotation.");
                            }
                        } else {
                            System.err.println("'" + node.getName() + "': unsupported nested arrays with non-primitive types");
                        }
                    }
                    case Type.Primitive typePrimitive -> {
                        var valueType = findType(typePrimitive.kind());
                        fieldSpecs.add(FieldSpec.builder(valueType.carrierClass(), node.getPrettyName()).build());
                    }
                    case Type.Declared typeDeclared -> {
                        var prettyName = lookupCallback.apply(typeDeclared.tree().name());
                        if (prettyName != null) {
                            fieldSpecs.add(FieldSpec.builder(ClassName.get("", prettyName), node.getPrettyName()).build());
                        } else {
                            System.err.println("Struct '" + node.getName() + "' is not present in parsing queue. Please, explicitly add struct in annotation.");
                        }
                    }
                    default -> System.err.println("Unknown type '" + type + "' for field '" + node.getName() + "'");
                }

                //var valueType = node.isArray() ? TYPE_TO_ARRAY_MAPPING.get(type.toString()) : TYPE_MAPPING.get(type.toString());
//                if (valueType != null) {
//                    fieldSpecs.add(FieldSpec.builder(valueType, node.getPrettyName()).build());
//                } else if (type instanceof Type.Declared typeDeclared) {
//                    var prettyName = lookupCallback.apply(typeDeclared.tree().name());
//                    if (prettyName != null) {
//                        fieldSpecs.add(FieldSpec.builder(ClassName.get("", prettyName + (node.isArray() ? "[]" : "")), node.getPrettyName()).build());
//                    } else {
//                        System.err.println("Struct '" + node.getName() + "' is not present in parsing queue. Please, explicitly add struct in annotation.");
//                    }
//                }
            }
        }
        return fieldSpecs;
    }

    private static List<CodeBlock> processEmptyConstructor(List<NativeMemoryNode> nodes, Function<String, String> lookupCallback) {
        List<CodeBlock> statements = new ArrayList<>();
        for (NativeMemoryNode node : nodes) {
            var type = node.getType();
            if (type == null) {
                statements.addAll(processEmptyConstructor(node.getNodes(), lookupCallback));
            } else {
                switch (type) {
                    case Type.Primitive typePrimitive -> {
                        var valueType = findType(typePrimitive.kind());
                        statements.add(CodeBlock.builder().add(valueType.newConstructor()).build());
                    }
                    case Type.Array typeArray -> {
                        var elementType = typeArray.elementType();
                        if (elementType instanceof Type.Primitive typePrimitive) {
                            var valueType = findType(typePrimitive.kind());
                            statements.add(CodeBlock.builder().add(valueType.newArrayConstructor()).build());
                        } else if (elementType instanceof Type.Declared typeDeclared) {
                            var prettyName = lookupCallback.apply(typeDeclared.tree().name());
                            if (prettyName != null) {
                                statements.add(CodeBlock.builder().add("new $L[]{}", prettyName).build());
                            } else {
                                System.err.println("Struct '" + node.getName() + "' is not present in parsing queue. Please, explicitly add struct in annotation.");
                            }
                        } else {
                            System.err.println("'" + node.getName() + "': unsupported nested arrays with non-primitive types");
                        }
                    }
                    case Type.Declared typeDeclared -> {
                        var prettyName = lookupCallback.apply(typeDeclared.tree().name());
                        if (prettyName != null) {
                            statements.add(CodeBlock.builder().add("$L.createEmpty()", prettyName).build());
                        } else {
                            System.err.println("Struct '" + node.getName() + "' is not present in parsing queue. Please, explicitly add struct in annotation.");
                        }
                    }
                    default -> System.err.println("Unknown type '" + type + "' for field '" + node.getName() + "'");
                }

//                if (node.isArray()) {
//                    var arrayValueType = TYPE_TO_ARRAY_MAPPING.get(type.toString());
//                    if (arrayValueType != null) {
//                        statements.add(CodeBlock.builder().add(EMPTY_VALUES_MAPPING.get(arrayValueType)).build());
//                    } else if (type instanceof Type.Declared typeDeclared) {
//                        var prettyName = lookupCallback.apply(typeDeclared.tree().name());
//                        if (prettyName != null) {
//                            statements.add(CodeBlock.builder().add("new $L[]{}", prettyName).build());
//                        } else {
//                            System.err.println("Struct '" + node.getName() + "' is not present in parsing queue. Please, explicitly add struct in annotation.");
//                        }
//                    }
//                } else {
//                    var valueType = TYPE_MAPPING.get(type.toString());
//                    if (valueType != null) {
//                        statements.add(CodeBlock.builder().add(EMPTY_VALUES_MAPPING.get(valueType)).build());
//                    } else if (type instanceof Type.Declared typeDeclared) {
//                        var prettyName = lookupCallback.apply(typeDeclared.tree().name());
//                        if (prettyName != null) {
//                            statements.add(CodeBlock.builder().add("$L.createEmpty()", prettyName).build());
//                        } else {
//                            System.err.println("Struct '" + node.getName() + "' is not present in parsing queue. Please, explicitly add struct in annotation.");
//                        }
//                    }
//
//                }
            }
        }
        return statements;
    }

    private static List<FieldSpec> processHandleFields(List<NativeMemoryNode> nodes, String nestedName, Function<String, String> lookupCallback) {
        List<FieldSpec> fieldSpecs = new ArrayList<>();
        for (NativeMemoryNode node : nodes) {
            var type = node.getType();
            if (type == null) {
                fieldSpecs.addAll(processHandleFields(node.getNodes(), node.getName(), lookupCallback));
            } else {
                var layoutString = nestedName != null ?
                        CodeBlock.builder().add("LAYOUT.select($T.groupElement($S))", MemoryLayout.PathElement.class, nestedName).build()
                        : CodeBlock.builder().add("LAYOUT").build();
                if (node.isArray()) {
                    fieldSpecs.add(
                            FieldSpec.builder(TypeName.get(MethodHandle.class), "MH_" + node.getName().toUpperCase())
                                    .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                                    .initializer("$L.sliceHandle(MemoryLayout.PathElement.groupElement($S))", layoutString, node.getName())
                                    .build()
                    );
                } else {
                    if (type instanceof Type.Declared typeDeclared) {
                        var prettyName = lookupCallback.apply(typeDeclared.tree().name());
                        if (prettyName != null) {
                            fieldSpecs.add(
                                    FieldSpec.builder(TypeName.get(MethodHandle.class), "MH_" + node.getName().toUpperCase())
                                            .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                                            .initializer("$L.sliceHandle(MemoryLayout.PathElement.groupElement($S))", layoutString, node.getName())
                                            .build());
                        } else {
                            System.err.println("Struct '" + node.getName() + "' is not present in parsing queue. Please, explicitly add struct in annotation.");
                        }
                    } else {
                        fieldSpecs.add(
                                FieldSpec.builder(TypeName.get(VarHandle.class), "VH_" + node.getName().toUpperCase())
                                        .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                                        .initializer("$L.varHandle(MemoryLayout.PathElement.groupElement($S))", layoutString, node.getName())
                                        .build());
                    }
                }
            }
        }
        return fieldSpecs;
    }

    private static List<CodeBlock> processFromBodyStatements(List<NativeMemoryNode> nodes, Function<String, String> lookupCallback) {
        List<CodeBlock> statements = new ArrayList<>();
        for (NativeMemoryNode node : nodes) {
            var type = node.getType();
            if (type == null) {
                statements.add(CodeBlock.builder()
                        .addStatement("var unionSize = LAYOUT.select($T.groupElement($S)).byteSize()", MemoryLayout.PathElement.class, node.getName())
                        .addStatement("var unionBuffer = buffer.asSlice(LAYOUT.byteSize() - unionSize, unionSize)")
                        .build());
                statements.addAll(processFromBodyStatements(node.getNodes(), lookupCallback));
            } else {
                if (node.isArray() && type instanceof Type.Declared typeDeclared) {
                    var prettyName = lookupCallback.apply(typeDeclared.tree().name());
                    if (prettyName != null) {
                        statements.add(CodeBlock.builder()
                                .addStatement("var $LMemorySegment = invokeExact(MH_$L, buffer)", node.getPrettyName(), node.getName().toUpperCase())
                                .addStatement("var $L = new $L[$L]", node.getPrettyName(), prettyName, node.getArraySize())
                                .beginControlFlow("for(int i = 0; i < $L; i++)", node.getArraySize())
                                .addStatement("var tmp = $L.createEmpty()", prettyName)
                                .addStatement("$L[i] = tmp.fromBytes($LMemorySegment.asSlice($L.LAYOUT.byteSize() * i, $L.LAYOUT.byteSize()))",
                                        node.getPrettyName(), node.getPrettyName(), prettyName, prettyName)
                                .endControlFlow()
                                .build());
                    } else {
                        System.err.println("Struct '" + node.getName() + "' is not present in parsing queue. Please, explicitly add struct in annotation.");
                    }
                }
            }
        }
        return statements;
    }

    private static List<CodeBlock> processFromReturnStatements(List<NativeMemoryNode> nodes, boolean nested, Function<String, String> lookupCallback) {
        List<CodeBlock> statements = new ArrayList<>();
        for (NativeMemoryNode node : nodes) {
            var type = node.getType();
            if (type == null) {
                statements.addAll(processFromReturnStatements(node.getNodes(), true, lookupCallback));
            } else {
                switch (type) {
                    case Type.Array typeArray -> {
                        if (typeArray.elementType() instanceof Type.Primitive typePrimitive) {
                            var valueType = findType(typePrimitive.kind());
                            statements.add(
                                    CodeBlock.builder()
                                            .add("invokeExact(MH_$L, $L).toArray($T.$L)", node.getName().toUpperCase(), nested ? "unionBuffer" : "buffer",
                                                    ValueLayout.class, valueType.valueLayoutName())
                                            .build());
                        } else if (typeArray.elementType() instanceof Type.Declared typeDeclared) {
                            var prettyName = lookupCallback.apply(typeDeclared.tree().name());
                            if (prettyName != null) {
                                statements.add(CodeBlock.builder().add("$L", node.getPrettyName()).build());
                            } else {
                                System.err.println("Struct '" + node.getName() + "' is not present in parsing queue. Please, explicitly add struct in annotation.");
                            }
                        } else {
                            System.err.println("'" + node.getName() + "': unsupported nested arrays with non-primitive types");
                        }
                    }
                    case Type.Primitive typePrimitive -> {
                        var valueType = findType(typePrimitive.kind());
                        statements.add(CodeBlock.builder().add("($T) VH_$L.get($L, 0L)", valueType.carrierClass(), node.getName().toUpperCase(), nested ? "unionBuffer" : "buffer").build());
                    }
                    case Type.Declared typeDeclared -> {
                        var prettyName = lookupCallback.apply(typeDeclared.tree().name());
                        if (prettyName != null) {
                            statements.add(CodeBlock.builder().add("$L", node.getPrettyName()).build());
                        } else {
                            System.err.println("Struct '" + node.getName() + "' is not present in parsing queue. Please, explicitly add struct in annotation.");
                        }
                    }
                    default -> System.err.println("Unknown type '" + type + "' for field '" + node.getName() + "'");
                }


//                if (node.isArray()) {
//                    var arrayValueType = TYPE_TO_ARRAY_MAPPING.get(type.toString());
//                    if (arrayValueType != null) {
//                        statements.add(
//                                CodeBlock.builder()
//                                        .add("invokeExact(MH_$L, $L).toArray($T.$L)", node.getName().toUpperCase(), nested ? "unionBuffer" : "buffer",
//                                                ValueLayout.class, MEMORY_LAYOUT_VALUES_MAPPING.get(type.toString()))
//                                        .build()
//                        );
//                    } else if (type instanceof Type.Declared typeDeclared) {
//                        var prettyName = lookupCallback.apply(typeDeclared.tree().name());
//                        if (prettyName != null) {
//                            statements.add(CodeBlock.builder().add("$L", node.getPrettyName()).build());
//                        } else {
//                            System.err.println("Struct '" + node.getName() + "' is not present in parsing queue. Please, explicitly add struct in annotation.");
//                        }
//                    }
//                } else {
//                    var valueType = TYPE_MAPPING.get(type.toString());
//                    if (valueType != null) {
//                        statements.add(
//                                CodeBlock.builder().add("($T) VH_$L.get($L, 0L)", valueType, node.getName().toUpperCase(), nested ? "unionBuffer" : "buffer").build()
//                        );
//                    } else if (type instanceof Type.Declared typeDeclared) {
//                        var prettyName = lookupCallback.apply(typeDeclared.tree().name());
//                        if (prettyName != null) {
//                            statements.add(CodeBlock.builder().add("$L.createEmpty().fromBytes(invokeExact(MH_$L, $L))", prettyName, node.getName().toUpperCase(), nested ? "unionBuffer" : "buffer").build());
//                        } else {
//                            System.err.println("Struct '" + node.getName() + "' is not present in parsing queue. Please, explicitly add struct in annotation.");
//                        }
//                    }
//                }
            }
        }
        return statements;
    }

    private static List<CodeBlock> processToBodyStatements(List<NativeMemoryNode> nodes, boolean nested, Function<String, String> lookupCallback) {
        List<CodeBlock> statements = new ArrayList<>();
        for (NativeMemoryNode node : nodes) {
            var type = node.getType();
            if (type == null) {
                statements.add(CodeBlock.builder()
                        .addStatement("var unionSize = LAYOUT.select($T.groupElement($S)).byteSize()", MemoryLayout.PathElement.class, node.getName())
                        .addStatement("var unionBuffer = buffer.asSlice(LAYOUT.byteSize() - unionSize, unionSize)")
                        .build());
                statements.addAll(processToBodyStatements(node.getNodes(), true, lookupCallback));
            } else {
                switch (type) {
                    case Type.Array typeArray -> {
                        if (typeArray.elementType() instanceof Type.Primitive typePrimitive) {
                            var valueType = findType(typePrimitive.kind());
                            var builder = CodeBlock.builder();
                            if (nested) {
                                builder.add(CodeBlock.builder().beginControlFlow("if ($L$L)", node.getPrettyName(), valueType.isArrayEmpty()).build());
                            }
                            builder.addStatement("var $LTmp = invokeExact(MH_$L, buffer)", node.getPrettyName(), node.getName().toUpperCase())
                                    .beginControlFlow("for (int i = 0; i < $L.length; i++)", node.getName())
                                    .addStatement("$LTmp.setAtIndex($T.$L, i, $L[i])", node.getPrettyName(), ValueLayout.class,
                                            valueType.valueLayoutName(), node.getPrettyName())
                                    .endControlFlow();
                            if (nested) {
                                builder.add(CodeBlock.builder().endControlFlow().build());
                            }
                            statements.add(builder.build());
                        } else if (typeArray.elementType() instanceof Type.Declared typeDeclared) {
                            var prettyName = lookupCallback.apply(typeDeclared.tree().name());
                            if (prettyName != null) {
                                var builder = CodeBlock.builder();
                                if (nested) {
                                    builder.add(CodeBlock.builder().beginControlFlow("if ($L.length > 0)", node.getPrettyName()).build());
                                }
                                builder.addStatement("var $LTmp = invokeExact(MH_$L, buffer)", node.getPrettyName(), node.getName().toUpperCase())
                                        .beginControlFlow("for (int i = 0; i < $L.length; i++)", node.getName())
                                        .addStatement("$L[i].toBytes($LTmp.asSlice($L.LAYOUT.byteSize() * i, $L.LAYOUT.byteSize()))",
                                                node.getPrettyName(), node.getPrettyName(), prettyName, prettyName)
                                        .endControlFlow();
                                if (nested) {
                                    builder.add(CodeBlock.builder().endControlFlow().build());
                                }
                                statements.add(builder.build());
                            } else {
                                System.err.println("Struct '" + node.getName() + "' is not present in parsing queue. Please, explicitly add struct in annotation.");
                            }
                        } else {
                            System.err.println("'" + node.getName() + "': unsupported nested arrays with non-primitive types");
                        }
                    }
                    case Type.Primitive typePrimitive -> {
                        var valueType = findType(typePrimitive.kind());
                        var builder = CodeBlock.builder();
                        if (nested) {
                            builder.add(CodeBlock.builder().beginControlFlow("if ($L$L)", node.getPrettyName(), valueType.isEmpty()).build());
                        }
                        builder.addStatement("VH_$L.set($L, 0L, $L)", node.getName().toUpperCase(), nested ? "unionBuffer" : "buffer", node.getPrettyName());
                        if (nested) {
                            builder.add(CodeBlock.builder().endControlFlow().build());
                        }
                        statements.add(builder.build());
                    }
                    case Type.Declared typeDeclared -> {
                        var prettyName = lookupCallback.apply(typeDeclared.tree().name());
                        if (prettyName != null) {
                            var builder = CodeBlock.builder();
                            if (nested) {
                                builder.add(CodeBlock.builder().beginControlFlow("if (!$L.isEmpty())", node.getPrettyName()).build());
                            }
                            builder.addStatement("var $LTmp = invokeExact(MH_$L, buffer)", node.getPrettyName(), node.getName().toUpperCase())
                                    .addStatement("$L.toBytes($LTmp)", node.getPrettyName(), node.getPrettyName());
                            if (nested) {
                                builder.add(CodeBlock.builder().endControlFlow().build());
                            }
                            statements.add(builder.build());
                        } else {
                            System.err.println("Struct '" + node.getName() + "' is not present in parsing queue. Please, explicitly add struct in annotation.");
                        }
                    }
                    default -> System.err.println("Unknown type '" + type + "' for field '" + node.getName() + "'");
                }

//                if (node.isArray()) {
//                    if (type instanceof Type.Declared typeDeclared) {
//                        var prettyName = lookupCallback.apply(typeDeclared.tree().name());
//                        if (prettyName != null) {
//                            var builder = CodeBlock.builder();
//                            if (nested) {
//                                Class<?> valueType;
//                                valueType = TYPE_MAPPING.get(type.toString());
//                                if (valueType == null) {
//                                    valueType = NativeMemoryLayout[].class;
//                                }
//                                builder.add(CodeBlock.builder().beginControlFlow("if ($L$L)", node.getPrettyName(), TYPE_EMPTY_MAPPING.get(valueType)).build());
//                            }
//                            builder.addStatement("var $LTmp = invokeExact(MH_$L, buffer)", node.getPrettyName(), node.getName().toUpperCase())
//                                    .beginControlFlow("for (int i = 0; i < $L.length; i++)", node.getName())
//                                    .addStatement("$L[i].toBytes($LTmp.asSlice($L.LAYOUT.byteSize() * i, $L.LAYOUT.byteSize()))",
//                                            node.getPrettyName(), node.getPrettyName(), prettyName, prettyName)
//                                    .endControlFlow();
//                            if (nested) {
//                                builder.add(CodeBlock.builder().endControlFlow().build());
//                            }
//                            statements.add(builder.build());
//                        } else {
//                            System.err.println("Struct '" + node.getName() + "' is not present in parsing queue. Please, explicitly add struct in annotation.");
//                        }
//                    } else {
//                        var builder = CodeBlock.builder();
//                        if (nested) {
//                            Class<?> valueType;
//                            valueType = TYPE_MAPPING.get(type.toString());
//                            if (valueType == null) {
//                                valueType = NativeMemoryLayout[].class;
//                            }
//                            builder.add(CodeBlock.builder().beginControlFlow("if ($L$L)", node.getPrettyName(), TYPE_EMPTY_MAPPING.get(valueType)).build());
//                        }
//                        builder.addStatement("var $LTmp = invokeExact(MH_$L, buffer)", node.getPrettyName(), node.getName().toUpperCase())
//                                .beginControlFlow("for (int i = 0; i < $L.length; i++)", node.getName())
//                                .addStatement("$LTmp.setAtIndex($T.$L, i, $L[i])", node.getPrettyName(), ValueLayout.class,
//                                        MEMORY_LAYOUT_VALUES_MAPPING.get(type.toString()), node.getPrettyName())
//                                .endControlFlow();
//                        if (nested) {
//                            builder.add(CodeBlock.builder().endControlFlow().build());
//                        }
//                        statements.add(builder.build());
//                    }
//                } else {
//                    if (type instanceof Type.Declared typeDeclared) {
//                        var prettyName = lookupCallback.apply(typeDeclared.tree().name());
//                        if (prettyName != null) {
//                            var builder = CodeBlock.builder();
//                            if (nested) {
//                                Class<?> valueType;
//                                valueType = TYPE_MAPPING.get(type.toString());
//                                if (valueType == null) {
//                                    valueType = NativeMemoryLayout.class;
//                                }
//                                builder.add(CodeBlock.builder().beginControlFlow("if ($L$L)", node.getPrettyName(), TYPE_EMPTY_MAPPING.get(valueType)).build());
//                            }
//                            builder.addStatement("var $LTmp = invokeExact(MH_$L, buffer)", node.getPrettyName(), node.getName().toUpperCase())
//                                    .addStatement("$L.toBytes($LTmp)", node.getPrettyName(), node.getPrettyName());
//                            if (nested) {
//                                builder.add(CodeBlock.builder().endControlFlow().build());
//                            }
//                            statements.add(builder.build());
//                        } else {
//                            System.err.println("Struct '" + node.getName() + "' is not present in parsing queue. Please, explicitly add struct in annotation.");
//                        }
//                    } else {
//                        var builder = CodeBlock.builder();
//                        if (nested) {
//                            Class<?> valueType;
//                            valueType = TYPE_MAPPING.get(type.toString());
//                            if (valueType == null) {
//                                valueType = NativeMemoryLayout.class;
//                            }
//                            builder.add(CodeBlock.builder().beginControlFlow("if ($L$L)", node.getPrettyName(), TYPE_EMPTY_MAPPING.get(valueType)).build());
//                        }
//                        builder.addStatement("VH_$L.set($L, 0L, $L)", node.getName().toUpperCase(), nested ? "unionBuffer" : "buffer", node.getPrettyName());
//                        if (nested) {
//                            builder.add(CodeBlock.builder().endControlFlow().build());
//                        }
//                        statements.add(builder.build());
//                    }
//                }
            }
        }
        return statements;
    }

    private static List<CodeBlock> processIsEmptyReturnStatements(List<NativeMemoryNode> nodes) {
        List<CodeBlock> statements = new ArrayList<>();
        for (NativeMemoryNode node : nodes) {
            var type = node.getType();
            if (type == null) {
                statements.addAll(processIsEmptyReturnStatements(node.getNodes()));
            } else {
                switch (type) {
                    case Type.Array typeArray -> {
                        if (typeArray.elementType() instanceof Type.Primitive typePrimitive) {
                            var valueType = findType(typePrimitive.kind());
                            statements.add(CodeBlock.builder().add("$L$L", node.getPrettyName(), valueType.isArrayEmpty()).build());
                        } else if (typeArray.elementType() instanceof Type.Declared) {
                            statements.add(CodeBlock.builder().add("$L.length > 0", node.getPrettyName()).build());
                        } else {
                            System.err.println("'" + node.getName() + "': unsupported nested arrays with non-primitive types");
                        }
                    }
                    case Type.Primitive typePrimitive -> {
                        var valueType = findType(typePrimitive.kind());
                        statements.add(CodeBlock.builder().add("$L$L", node.getPrettyName(), valueType.isEmpty()).build());
                    }
                    case Type.Declared _ -> {
                        statements.add(CodeBlock.builder().add("$L.isEmpty()", node.getPrettyName()).build());
                    }
                    default -> System.err.println("Unknown type '" + type + "' for field '" + node.getName() + "'");
                }
//                Class<?> valueType;
//                if (node.isArray()) {
//                    valueType = TYPE_TO_ARRAY_MAPPING.get(type.toString());
//                    if (valueType == null) {
//                        valueType = NativeMemoryLayout[].class;
//                    }
//                } else {
//                    valueType = TYPE_MAPPING.get(type.toString());
//                    if (valueType == null) {
//                        valueType = NativeMemoryLayout.class;
//                    }
//                }
//                statements.add(CodeBlock.builder().add("$L$L", node.getPrettyName(), TYPE_EMPTY_MAPPING.get(valueType)).build());
            }
        }
        return statements;
    }
}

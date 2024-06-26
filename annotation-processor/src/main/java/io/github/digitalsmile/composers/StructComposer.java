package io.github.digitalsmile.composers;

import com.squareup.javapoet.*;
import io.github.digitalsmile.parser.NativeMemoryModel;
import io.github.digitalsmile.parser.NativeMemoryNode;
import io.github.digitalsmile.parser.Parser;
import io.github.digitalsmile.type.PrimitiveTypeMapping;
import io.github.digitalsmile.type.TypeMapping;
import io.github.digitalsmile.annotation.structure.NativeMemoryLayout;
import org.openjdk.jextract.Type;

import javax.lang.model.element.Modifier;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.openjdk.jextract.Type.Primitive.Kind.*;

public class StructComposer {

    public static final List<TypeMapping> PRIMITIVE_MAPPING = List.of(
            new PrimitiveTypeMapping(Int, ValueLayout.JAVA_INT),
            new PrimitiveTypeMapping(List.of(Char, Char16), ValueLayout.JAVA_BYTE),
            new PrimitiveTypeMapping(List.of(Long, LongLong), ValueLayout.JAVA_LONG),
            new PrimitiveTypeMapping(Short, ValueLayout.JAVA_SHORT),
            new PrimitiveTypeMapping(Float, ValueLayout.JAVA_FLOAT),
            new PrimitiveTypeMapping(Double, ValueLayout.JAVA_DOUBLE),
            new PrimitiveTypeMapping(Void, null)
    );

    public static String compose(String type, NativeMemoryModel nativeMemoryModel, String packageName, String prettyName, Function<String, String> lookupCallback) {
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
                                                .add("$T.$LLayout(\n\t", MemoryLayout.class, type)
                                                .add(CodeBlock.join(memoryLayout, ",\n\t"))
                                                .add("\n)")
                                                .build())
                                .build())
                        .addFields(handlesFields)
                        .addMethod(MethodSpec.methodBuilder("createEmpty")
                                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                .returns(ClassName.get(packageName, prettyName))
                                .addStatement("return new $N($L)", prettyName, CodeBlock.join(emptyConstructorStatements, ", ")).build())
                        .addMethod(MethodSpec.methodBuilder("getMemoryLayout")
                                .addModifiers(Modifier.PUBLIC)
                                .addAnnotation(AnnotationSpec.builder(Override.class).build())
                                .returns(TypeName.get(MemoryLayout.class))
                                .addCode(CodeBlock.builder().addStatement("return LAYOUT").build())
                                .build())
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
        return PRIMITIVE_MAPPING.stream().filter(list -> list.types().contains(type)).findFirst().orElseThrow();
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
                                .add("\n\t).withName($S)", node.getPrettyName())
                                .build());
            } else {
                switch (type) {
                    case Type.Array typeArray -> {
                        if (typeArray.elementType() instanceof Type.Primitive typePrimitive) {
                            var valueLayout = findType(typePrimitive.kind());
                            memoryLayouts.add(
                                    CodeBlock.builder().add("$T.sequenceLayout($L, $T.$L).withName($S)", MemoryLayout.class,
                                                    typeArray.elementCount().orElseThrow(), ValueLayout.class, valueLayout.valueLayoutName(), node.getPrettyName())
                                            .build());
                        } else if (typeArray.elementType() instanceof Type.Declared typeDeclared) {
                            var prettyName = lookupCallback.apply(typeDeclared.tree().name());
                            if (prettyName != null) {
                                memoryLayouts.add(CodeBlock.builder().add("$T.sequenceLayout($L, $L.LAYOUT).withName($S)", MemoryLayout.class,
                                                typeArray.elementCount().orElseThrow(), prettyName, node.getPrettyName())
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
                        memoryLayouts.add(CodeBlock.builder().add("$T.$L.withName($S)", ValueLayout.class, valueLayout.valueLayoutName(), node.getPrettyName()).build());
                    }
                    case Type.Declared typeDeclared -> {
                        var prettyName = lookupCallback.apply(typeDeclared.tree().name());
                        if (prettyName != null) {
                            memoryLayouts.add(CodeBlock.builder().add("$L.LAYOUT.withName($S)", prettyName, node.getPrettyName()).build());
                        } else {
                            System.err.println("Struct '" + node.getName() + "' is not present in parsing queue. Please, explicitly add struct in annotation.");
                        }
                    }
                    default -> System.err.println("Unknown type '" + type + "' for field '" + node.getName() + "'");
                }
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
            }
        }
        return statements;
    }

    private static List<FieldSpec> processHandleFields(List<NativeMemoryNode> nodes, String nestedName, Function<String, String> lookupCallback) {
        List<FieldSpec> fieldSpecs = new ArrayList<>();
        for (NativeMemoryNode node : nodes) {
            var type = node.getType();
            if (type == null) {
                fieldSpecs.addAll(processHandleFields(node.getNodes(), node.getPrettyName(), lookupCallback));
            } else {
                var layoutCodeBlock = nestedName != null ?
                        CodeBlock.builder().add("LAYOUT.select($T.groupElement($S))", MemoryLayout.PathElement.class, nestedName).build()
                        : CodeBlock.builder().add("LAYOUT").build();
                switch (type) {
                    case Type.Array array ->
                            fieldSpecs.add(FieldSpec.builder(TypeName.get(MethodHandle.class), "MH_" + node.getName().toUpperCase())
                                    .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                                    .initializer("$L.sliceHandle(MemoryLayout.PathElement.groupElement($S))", layoutCodeBlock, node.getPrettyName())
                                    .build());
                    case Type.Declared typeDeclared -> {
                        var prettyName = lookupCallback.apply(typeDeclared.tree().name());
                        if (prettyName != null) {
                            fieldSpecs.add(FieldSpec.builder(TypeName.get(MethodHandle.class), "MH_" + node.getName().toUpperCase())
                                    .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                                    .initializer("$L.sliceHandle(MemoryLayout.PathElement.groupElement($S))", layoutCodeBlock, node.getPrettyName())
                                    .build());
                        } else {
                            System.err.println("Struct '" + node.getName() + "' is not present in parsing queue. Please, explicitly add struct in annotation.");
                        }
                    }
                    case Type.Primitive primitive ->
                            fieldSpecs.add(FieldSpec.builder(TypeName.get(VarHandle.class), "VH_" + node.getName().toUpperCase())
                                    .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                                    .initializer("$L.varHandle(MemoryLayout.PathElement.groupElement($S))", layoutCodeBlock, node.getPrettyName())
                                    .build());
                    default -> System.err.println("Unknown type '" + type + "' for field '" + node.getName() + "'");
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
                        .addStatement("var unionSize = LAYOUT.select($T.groupElement($S)).byteSize()", MemoryLayout.PathElement.class, node.getPrettyName())
                        .addStatement("var unionBuffer = buffer.asSlice(LAYOUT.byteSize() - unionSize, unionSize)")
                        .build());
                statements.addAll(processFromBodyStatements(node.getNodes(), lookupCallback));
            } else {
                if (type instanceof Type.Array typeArray) {
                    if (typeArray.elementType() instanceof Type.Declared typeDeclared) {
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
                } else if (type instanceof Type.Declared typeDeclared) {
                    var prettyName = lookupCallback.apply(typeDeclared.tree().name());
                    if (prettyName != null) {
                        statements.add(CodeBlock.builder()
                                .addStatement("var $LMemorySegment = invokeExact(MH_$L, buffer)", node.getPrettyName(), node.getName().toUpperCase())
                                .addStatement("var $L = $L.createEmpty().fromBytes($LMemorySegment)", node.getPrettyName(), prettyName, node.getPrettyName())
                                .build());
                    } else {
                        System.err.println("Struct '" + node.getName() + "' is not present in parsing queue. Please, explicitly add struct in annotation.");
                    }
                }
            }
        }
        return statements;
    }

    private static List<CodeBlock> processFromReturnStatements(List<NativeMemoryNode> nodes,
                                                               boolean nested, Function<String, String> lookupCallback) {
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
            }
        }
        return statements;
    }

    private static List<CodeBlock> processToBodyStatements(List<NativeMemoryNode> nodes, boolean nested, Function<
            String, String> lookupCallback) {
        List<CodeBlock> statements = new ArrayList<>();
        for (NativeMemoryNode node : nodes) {
            var type = node.getType();
            if (type == null) {
                statements.add(CodeBlock.builder()
                        .addStatement("var unionSize = LAYOUT.select($T.groupElement($S)).byteSize()", MemoryLayout.PathElement.class, node.getPrettyName())
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
                                builder.add(CodeBlock.builder().beginControlFlow("if ($L$L)", node.getPrettyName(), valueType.isNotArrayEmpty()).build());
                            }
                            builder.addStatement("var $LTmp = invokeExact(MH_$L, buffer)", node.getPrettyName(), node.getName().toUpperCase())
                                    .beginControlFlow("for (int i = 0; i < $L.length; i++)", node.getPrettyName())
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
                                        .beginControlFlow("for (int i = 0; i < $L.length; i++)", node.getPrettyName())
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
                            builder.add(CodeBlock.builder().beginControlFlow("if ($L$L)", node.getPrettyName(), valueType.isNotEmpty()).build());
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
                            builder.addStatement("var $LTmp = invokeExact(MH_$L, buffer)", node.getPrettyName(), node.getPrettyName().toUpperCase())
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
            }
        }
        return statements;
    }
}

package io.github.digitalsmile.composers;

import com.squareup.javapoet.*;
import io.github.digitalsmile.PackageName;
import io.github.digitalsmile.PrettyName;
import io.github.digitalsmile.annotation.types.interfaces.NativeMemoryLayout;
import io.github.digitalsmile.headers.mapping.ArrayOriginalType;
import io.github.digitalsmile.headers.mapping.ObjectOriginalType;
import io.github.digitalsmile.headers.mapping.OriginalType;
import io.github.digitalsmile.headers.mapping.PrimitiveOriginalType;
import io.github.digitalsmile.headers.model.NativeMemoryNode;
import io.github.digitalsmile.headers.model.NodeType;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Modifier;
import javax.tools.Diagnostic;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.List;

public class StructComposer {
    private final Messager messager;
    private final boolean isUnion;

    public StructComposer(Messager messager) {
        this(messager, false);
    }

    public StructComposer(Messager messager, boolean isUnion) {
        this.messager = messager;
        this.isUnion = isUnion;
    }

    public String compose(String prettyName, NativeMemoryNode node) {
        var packageName = PackageName.getPackageName(node.getName());
        var record = TypeSpec.recordBuilder(prettyName)
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(TypeName.get(NativeMemoryLayout.class))
                .addJavadoc("Source: $L$L", node.getPosition(), node.getPosition().comment().isEmpty() ? "" : "\n\n")
                .addJavadoc(node.getPosition().comment())
                .addFields(processConstructorParameters(node.nodes()))
                .addField(FieldSpec.builder(TypeName.get(MemoryLayout.class), "LAYOUT", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer(
                                CodeBlock.builder()
                                        .add("$T.$LLayout(\n\t", MemoryLayout.class, isUnion ? "union" : "struct")
                                        .add(CodeBlock.join(processMemoryLayout(node.nodes(), node), ",\n\t"))
                                        .add("\n)")
                                        .build())
                        .build())
                .addFields(processHandleFields(node.nodes()))
                .addMethod(MethodSpec.methodBuilder("create")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .addParameter(MemorySegment.class, "memorySegment")
                        .returns(ClassName.get(packageName, prettyName))
                        .addException(Throwable.class)
                        .addStatement("var $LInstance = $T.createEmpty()", PrettyName.getVariableName(node.getName()), ClassName.get(packageName, prettyName))
                        .beginControlFlow("if (!memorySegment.equals($T.NULL))", MemorySegment.class)
                        .addStatement("$LInstance = $LInstance.fromBytes(memorySegment)", PrettyName.getVariableName(node.getName()), PrettyName.getVariableName(node.getName()))
                        .endControlFlow()
                        .addStatement("return $LInstance", PrettyName.getVariableName(node.getName()))
                        .build())
                .addMethod(MethodSpec.methodBuilder("createEmpty")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(ClassName.get(packageName, prettyName))
                        .addStatement("return new $N($L)", prettyName, CodeBlock.join(processEmptyConstructor(node.nodes(), node), ", ")).build())
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
                        .addParameter(TypeName.get(MemorySegment.class), "memoryBufferSegment")
                        .addException(TypeName.get(Throwable.class))
                        .returns(ClassName.get(packageName, prettyName))
                        .addCode(CodeBlock.join(processFromBodyStatements(node.nodes(), node), ""))
                        .addCode("return new $L(\n\t", ClassName.get(packageName, prettyName).simpleName())
                        .addCode(CodeBlock.join(processFromReturnStatements(node.nodes(), node), ",\n\t"))
                        .addCode(");")
                        .build())
                .addMethod(MethodSpec.methodBuilder("toBytes")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(AnnotationSpec.builder(Override.class).build())
                        .addParameter(TypeName.get(MemorySegment.class), "memoryBufferSegment")
                        .addException(TypeName.get(Throwable.class))
                        .returns(TypeName.VOID)
                        .addCode(CodeBlock.join(processToBodyStatements(node.nodes(), node), ""))
                        .build())
                .addMethod(MethodSpec.methodBuilder("isEmpty")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(AnnotationSpec.builder(Override.class).build())
                        .returns(TypeName.BOOLEAN.unbox())
                        .addCode(CodeBlock.builder().addStatement("return $L", CodeBlock.join(processIsEmptyReturnStatements(node.nodes()), " && ")).build())
                        .build())
                .build();

        var outputFile = JavaFile.builder(packageName, record)
                .addStaticImport(MemoryLayout.PathElement.class, "*")
                .indent("\t").skipJavaLangImports(true).build();
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

    private List<FieldSpec> processConstructorParameters(List<NativeMemoryNode> nodes) {
        List<FieldSpec> fieldSpecs = new ArrayList<>();
        for (NativeMemoryNode node : nodes) {
            var prettyName = PrettyName.getVariableName(node.getName());
            var type = node.getType();
            switch (type) {
                case ArrayOriginalType arrayType -> {
                    if (arrayType.isObjectType()) {
                        fieldSpecs.add(FieldSpec.builder(createType(arrayType, true), prettyName).build());
                    } else {
                        fieldSpecs.add(FieldSpec.builder(arrayType.carrierClass().arrayType(), prettyName).build());
                    }
                }
                case PrimitiveOriginalType primitiveType ->
                        fieldSpecs.add(FieldSpec.builder(primitiveType.carrierClass(), prettyName).build());
                case ObjectOriginalType objectType -> {
                    if (node.getNodeType().isAnonymous()) {
                        fieldSpecs.addAll(processConstructorParameters(node.nodes()));
                    } else {
                        fieldSpecs.add(FieldSpec.builder(createType(objectType), prettyName).build());
                    }
                }
                default -> messager.printMessage(Diagnostic.Kind.ERROR, "unsupported type " + type);
            }
        }
        return fieldSpecs;
    }

    private long calculateMaxAlignment(List<NativeMemoryNode> nodes) {
        return nodes.stream().map(node ->
                        node.nodes().isEmpty() ? node.getType().valueLayout().byteAlignment() : calculateMaxAlignment(node.nodes()))
                .max(Long::compareTo)
                .orElse(8L);
    }

    private List<CodeBlock> processMemoryLayout(List<NativeMemoryNode> nodes, NativeMemoryNode n) {
        List<CodeBlock> memoryLayouts = new ArrayList<>();
        //var maxAlignment = calculateMaxAlignment(nodes);
        var sumAlignment = 0;
        for (NativeMemoryNode node : nodes) {
            var type = node.getType();
            var comment = node.getPosition().comment();
            var builder = CodeBlock.builder();
            if (!comment.isEmpty()) {
                var comments = comment.split("\n");
                for (var comm : comments) {
                    memoryLayouts.add(CodeBlock.builder().add("// $L", comm).build());
                }
            }
            switch (type) {
                case ArrayOriginalType arrayType -> {
                    if (arrayType.isObjectType()) {
                        memoryLayouts.add(builder.add("$T.sequenceLayout($L, $L.LAYOUT).withName($S)", MemoryLayout.class,
                                        arrayType.arraySize(), PrettyName.getObjectName(arrayType.typeName()), node.getName())
                                .build());
                    } else {
                        memoryLayouts.add(builder.add("$T.sequenceLayout($L, $T.$L).withName($S)", MemoryLayout.class,
                                        arrayType.arraySize(), ValueLayout.class, arrayType.valueLayoutName(), node.getName())
                                .build());
                    }
                }
                case PrimitiveOriginalType primitiveType ->
                        memoryLayouts.add(builder.add("$T.$L.withName($S)", ValueLayout.class, primitiveType.valueLayoutName(), node.getName()).build());
                case ObjectOriginalType objectType -> {
                    if (node.getNodeType().equals(NodeType.ANON_UNION)) {
                        memoryLayouts.add(
                                builder
                                        .add("$T.unionLayout(\n\t\t", MemoryLayout.class)
                                        .add(CodeBlock.join(processMemoryLayout(node.nodes(), n), ",\n\t\t"))
                                        .add("\n\t).withName($S)", node.getName())
                                        .build());
                    } else {
                        if (objectType.carrierClass().equals(String.class) || node.getNodeType().equals(NodeType.POINTER)) {
                            memoryLayouts.add(builder.add("$T.ADDRESS.withName($S)", ValueLayout.class, node.getName()).build());
                        } else {
                            memoryLayouts.add(builder.add("$T.LAYOUT.withName($S)", createType(objectType), node.getName()).build());
                        }
                    }
                }
                default -> messager.printMessage(Diagnostic.Kind.ERROR, "unsupported type " + type);
            }
            var byteSize = type instanceof ArrayOriginalType originalType ? originalType.arraySize() * type.valueLayout().byteSize() : type.valueLayout().byteSize();
            sumAlignment += (int) byteSize;
            if (sumAlignment % 4 != 0) {
                var alignSize = (sumAlignment + 4 - 1) / 4 * 4;
                memoryLayouts.add(CodeBlock.builder().add("$T.paddingLayout($L)", MemoryLayout.class,
                        alignSize - sumAlignment).build());
            }
        }
        return memoryLayouts;
    }


    private List<FieldSpec> processHandleFields(List<NativeMemoryNode> nodes) {
        List<FieldSpec> fieldSpecs = new ArrayList<>();
        for (NativeMemoryNode node : nodes) {
            var type = node.getType();
            switch (type) {
                case ArrayOriginalType _ ->
                        fieldSpecs.add(FieldSpec.builder(MethodHandle.class, "MH_" + node.getName().toUpperCase())
                                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                                .initializer("LAYOUT.sliceHandle(groupElement($S))", node.getName())
                                .build());
                case PrimitiveOriginalType _ ->
                        fieldSpecs.add(FieldSpec.builder(VarHandle.class, "VH_" + node.getName().toUpperCase())
                                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                                .initializer("LAYOUT.varHandle(groupElement($S))", node.getName())
                                .build());
                case ObjectOriginalType _ -> {
                    if (node.getNodeType().isAnonymous()) {
                        var fields = processHandleFields(node.nodes());
                        for (FieldSpec spec : fields) {
                            var initializer = spec.initializer.toString();
                            var prefix = spec.type.toString().equals(VarHandle.class.getName()) ? "VH" : "MH";
                            fieldSpecs.add(FieldSpec.builder(prefix.equals("VH") ? VarHandle.class : MethodHandle.class,
                                            prefix + spec.name.substring(spec.name.indexOf("_")))
                                    .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                                    .initializer("LAYOUT.select(groupElement($S))$L", node.getName(),
                                            initializer.substring(initializer.indexOf(".")))
                                    .build());
                        }
                    } else {
                        fieldSpecs.add(FieldSpec.builder(MethodHandle.class, "MH_" + node.getName().toUpperCase())
                                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                                .initializer("LAYOUT.sliceHandle(groupElement($S))", node.getName())
                                .build());
                    }
                }
                default -> messager.printMessage(Diagnostic.Kind.ERROR, "unsupported type " + type);
            }
        }
        return fieldSpecs;
    }

    private List<CodeBlock> processEmptyConstructor(List<NativeMemoryNode> nodes, NativeMemoryNode parentNode) {
        List<CodeBlock> statements = new ArrayList<>();
        for (NativeMemoryNode node : nodes) {
            var type = node.getType();
            var builder = CodeBlock.builder();
            switch (type) {
                case ArrayOriginalType arrayType -> {
                    if (arrayType.isObjectType()) {
                        statements.add(builder.add("new $L[]{}", PrettyName.getObjectName(type.typeName())).build());
                    } else {
                        statements.add(builder.add(arrayType.newArrayConstructor()).build());
                    }
                }
                case PrimitiveOriginalType primitiveType ->
                        statements.add(builder.add(primitiveType.newConstructor()).build());
                case ObjectOriginalType objectType -> {
                    if (node.getNodeType().isAnonymous()) {
                        var codeBlocks = processEmptyConstructor(node.nodes(), node);
                        statements.addAll(codeBlocks);
                    } else {
                        if (objectType.carrierClass().equals(String.class)) {
                            statements.add(builder.add("\"\"").build());
                        } else if (objectType.typeName().equals(parentNode.getName())) {
                            statements.add(builder.add("null").build());
                        } else {
                            statements.add(builder.add("$T.createEmpty()", createType(objectType)).build());
                        }
                    }
                }
                default -> messager.printMessage(Diagnostic.Kind.ERROR, "unsupported type " + type);
            }
        }
        return statements;
    }

    private List<CodeBlock> processFromBodyStatements(List<NativeMemoryNode> nodes, NativeMemoryNode parentNode) {
        List<CodeBlock> statements = new ArrayList<>();
        for (NativeMemoryNode node : nodes) {
            var type = node.getType();
            var prettyName = PrettyName.getVariableName(node.getName());
            var builder = CodeBlock.builder();
            switch (type) {
                case ArrayOriginalType arrayType -> {
                    if (arrayType.isObjectType()) {
                        var prettyObjectName = PrettyName.getObjectName(type.typeName());
                        statements.add(builder
                                .addStatement("var $LMemorySegment = invokeExact(MH_$L, memoryBufferSegment)", prettyName, node.getName().toUpperCase())
                                .addStatement("var $L = new $L[$L]", prettyName, prettyObjectName, arrayType.arraySize())
                                .beginControlFlow("for(int i = 0; i < $L; i++)", arrayType.arraySize())
                                .addStatement("var tmp = $L.createEmpty()", prettyObjectName)
                                .addStatement("$L[i] = tmp.fromBytes($LMemorySegment.asSlice($L.LAYOUT.byteSize() * i, $L.LAYOUT.byteSize()))",
                                        prettyName, prettyName, prettyObjectName, prettyObjectName)
                                .endControlFlow()
                                .build());
                    }
                }
                case PrimitiveOriginalType _ -> {
                }
                case ObjectOriginalType objectType -> {
                    if (node.getNodeType().isAnonymous()) {
                        statements.add(builder
                                .addStatement("var $LSize = LAYOUT.select($T.groupElement($S)).byteSize()", prettyName, MemoryLayout.PathElement.class, node.getName())
                                .addStatement("var $LBuffer = memoryBufferSegment.asSlice(LAYOUT.byteSize() - $LSize, $LSize)", prettyName, prettyName, prettyName)
                                .build());

                        for (NativeMemoryNode innerNode : node.nodes()) {
                            if (innerNode.getType() instanceof ObjectOriginalType) {
                                var prettyInnerName = PrettyName.getVariableName(innerNode.getName());
                                var typeName = createType(innerNode.getType());
                                statements.add(CodeBlock.builder()
                                        .addStatement("var $LMemorySegment = invokeExact(MH_$L, $LBuffer)", prettyInnerName, innerNode.getName().toUpperCase(), prettyName)
                                        .addStatement("var $L = $T.createEmpty().fromBytes($LMemorySegment)", prettyInnerName,
                                                typeName, prettyInnerName)
                                        .build());
                            }
                        }
                    } else {
                        if (objectType.carrierClass().equals(String.class)) {
                            statements.add(builder
                                    .addStatement("var $LMemorySegment = invokeExact(MH_$L, memoryBufferSegment)", prettyName, node.getName().toUpperCase())
                                    .addStatement("var $L = $LMemorySegment.reinterpret(Integer.MAX_VALUE).getString(0)", prettyName, prettyName)
                                    .build());
                        } else {
                            builder.addStatement("var $LMemorySegment = invokeExact(MH_$L, memoryBufferSegment)", prettyName, node.getName().toUpperCase());
                            if (objectType.typeName().equals(parentNode.getName())) {
                                builder.addStatement("$L $L = null", PrettyName.getObjectName(objectType.typeName()), prettyName);
                                builder.beginControlFlow("if (!$LMemorySegment.get($T.ADDRESS, 0).equals($T.NULL))", prettyName, ValueLayout.class, MemorySegment.class);
                                builder.addStatement("$L = $L.createEmpty().fromBytes($LMemorySegment.reinterpret(LAYOUT.byteSize()))", prettyName, PrettyName.getObjectName(objectType.typeName()), prettyName)
                                        .endControlFlow();
                                statements.add(builder.build());
                            } else {
                                var typeName = createType(objectType);
                                statements.add(builder
                                        .addStatement("var $L = $T.createEmpty().fromBytes($LMemorySegment)", prettyName, typeName, prettyName)
                                        .build());
                            }
                        }
                    }
                }
                default -> messager.printMessage(Diagnostic.Kind.ERROR, "unsupported type " + type);
            }
        }
        return statements;
    }

    private List<CodeBlock> processFromReturnStatements(List<NativeMemoryNode> nodes, NativeMemoryNode parentNode) {
        List<CodeBlock> statements = new ArrayList<>();
        for (NativeMemoryNode node : nodes) {
            var type = node.getType();
            var bufferName = parentNode.getNodeType().isAnonymous() ?
                    CodeBlock.builder().add("$LBuffer", PrettyName.getVariableName(parentNode.getName())).build() :
                    CodeBlock.builder().add("memoryBufferSegment").build();
            var builder = CodeBlock.builder();
            var prettyName = PrettyName.getVariableName(node.getName());
            switch (type) {
                case ArrayOriginalType arrayType -> {
                    if (arrayType.isObjectType()) {
                        statements.add(builder.add("$L", prettyName).build());
                    } else {
                        statements.add(builder.add("invokeExact(MH_$L, $L).toArray($T.$L)", node.getName().toUpperCase(), bufferName,
                                        ValueLayout.class, arrayType.valueLayoutName())
                                .build());
                    }
                }
                case PrimitiveOriginalType primitiveOriginalType ->
                        statements.add(builder.add("($T) VH_$L.get($L, 0L)", primitiveOriginalType.carrierClass(), node.getName().toUpperCase(), bufferName).build());
                case ObjectOriginalType _ -> {
                    if (node.getNodeType().isAnonymous()) {
                        var codeBlocks = processFromReturnStatements(node.nodes(), node);
                        statements.addAll(codeBlocks);
                    } else {
                        statements.add(builder.add("$L", prettyName).build());
                    }
                }
                default -> messager.printMessage(Diagnostic.Kind.ERROR, "unsupported type " + type);
            }
        }
        return statements;
    }

    private List<CodeBlock> processToBodyStatements(List<NativeMemoryNode> nodes, NativeMemoryNode parentNode) {
        List<CodeBlock> statements = new ArrayList<>();
        var parentNodeType = parentNode.getNodeType();
        for (NativeMemoryNode node : nodes) {
            var type = node.getType();
            var prettyVariableName = PrettyName.getVariableName(node.getName());
            switch (type) {
                case ArrayOriginalType arrayType -> {
                    if (arrayType.isObjectType()) {
                        var builder = CodeBlock.builder();
                        if (parentNodeType.isAnonymous()) {
                            builder.add(CodeBlock.builder().beginControlFlow("if ($L.length > 0)", prettyVariableName).build());
                        }
                        builder.addStatement("var $LTmp = invokeExact(MH_$L, memoryBufferSegment)", prettyVariableName, node.getName().toUpperCase())
                                .beginControlFlow("for (int i = 0; i < $L.length; i++)", prettyVariableName)
                                .addStatement("$L[i].toBytes($LTmp.asSlice($L.LAYOUT.byteSize() * i, $L.LAYOUT.byteSize()))",
                                        prettyVariableName, prettyVariableName, PrettyName.getObjectName(arrayType.typeName()), PrettyName.getObjectName(arrayType.typeName()))
                                .endControlFlow();
                        if (parentNodeType.isAnonymous()) {
                            builder.add(CodeBlock.builder().endControlFlow().build());
                        }
                        statements.add(builder.build());
                    } else {
                        var builder = CodeBlock.builder();
                        if (parentNodeType.isAnonymous()) {
                            builder.add(CodeBlock.builder().beginControlFlow("if ($L$L)", prettyVariableName, arrayType.isNotArrayEmpty()).build());
                        }
                        builder.addStatement("var $LTmp = invokeExact(MH_$L, memoryBufferSegment)", prettyVariableName, node.getName().toUpperCase())
                                .beginControlFlow("for (int i = 0; i < $L.length; i++)", prettyVariableName)
                                .addStatement("$LTmp.setAtIndex($T.$L, i, $L[i])", prettyVariableName, ValueLayout.class,
                                        arrayType.valueLayoutName(), prettyVariableName)
                                .endControlFlow();
                        if (parentNodeType.isAnonymous()) {
                            builder.add(CodeBlock.builder().endControlFlow().build());
                        }
                        statements.add(builder.build());
                    }
                }
                case PrimitiveOriginalType primitiveType -> {
                    var builder = CodeBlock.builder();
                    if (parentNodeType.isAnonymous()) {
                        builder.add(CodeBlock.builder().beginControlFlow("if ($L$L)", prettyVariableName, primitiveType.isNotEmpty()).build());
                    }
                    var bufferName = node.getNodeType().isAnonymous() ?
                            CodeBlock.builder().add("$LBuffer", PrettyName.getVariableName(node.getName())).build() :
                            CodeBlock.builder().add("memoryBufferSegment").build();
                    builder.addStatement("VH_$L.set($L, 0L, $L)", node.getName().toUpperCase(), bufferName, prettyVariableName);
                    if (parentNodeType.isAnonymous()) {
                        builder.add(CodeBlock.builder().endControlFlow().build());
                    }
                    statements.add(builder.build());
                }
                case ObjectOriginalType objectType -> {
                    if (node.getNodeType().isAnonymous()) {
                        var codeBlocks = processToBodyStatements(node.nodes(), node);
                        statements.addAll(codeBlocks);
                    } else {
                        if (parentNodeType.isAnonymous()) {
                            var builder = CodeBlock.builder();
                            builder.add(CodeBlock.builder().beginControlFlow("if (!$L.isEmpty())", prettyVariableName).build());
                            builder.addStatement("var $LTmp = invokeExact(MH_$L, memoryBufferSegment)", prettyVariableName, node.getName().toUpperCase())
                                    .addStatement("$L.toBytes($LTmp)", prettyVariableName, prettyVariableName);
                            builder.add(CodeBlock.builder().endControlFlow().build());
                            statements.add(builder.build());
                        } else {
                            var builder = CodeBlock.builder();
                            if (objectType.carrierClass().equals(String.class)) {
                                builder.addStatement("var $LTmp = invokeExact(MH_$L, memoryBufferSegment)", prettyVariableName, node.getName().toUpperCase())
                                        .addStatement("$LTmp.setString(0, $L)", prettyVariableName, prettyVariableName);
                            } else {
                                builder.addStatement("var $LTmp = invokeExact(MH_$L, memoryBufferSegment)", prettyVariableName, node.getName().toUpperCase());
                                if (type.typeName().equals(parentNode.getName())) {
                                    builder.beginControlFlow("if ($L != null)", prettyVariableName);
                                    builder.addStatement("$L.toBytes($LTmp.reinterpret(LAYOUT.byteSize()))", prettyVariableName, prettyVariableName);
                                } else {
                                    builder.addStatement("$L.toBytes($LTmp)", prettyVariableName, prettyVariableName);
                                }

                                if (type.typeName().equals(parentNode.getName())) {
                                    builder.nextControlFlow("else");
                                    builder.addStatement("$LTmp.set($T.ADDRESS, 0, $T.NULL)", prettyVariableName, ValueLayout.class, MemorySegment.class);
                                    builder.endControlFlow();
                                }
                            }
                            statements.add(builder.build());
                        }
                    }
                }
                default -> messager.printMessage(Diagnostic.Kind.ERROR, "unsupported type " + type);
            }
        }
        return statements;
    }

    private List<CodeBlock> processIsEmptyReturnStatements(List<NativeMemoryNode> nodes) {
        List<CodeBlock> statements = new ArrayList<>();
        for (NativeMemoryNode node : nodes) {
            var type = node.getType();
            var prettyName = PrettyName.getVariableName(node.getName());
            var builder = CodeBlock.builder();
            switch (type) {
                case ArrayOriginalType arrayType -> {
                    if (arrayType.isObjectType()) {
                        statements.add(builder.add("$L.length > 0", prettyName).build());
                    } else {
                        statements.add(builder.add("$L$L", prettyName, arrayType.isArrayEmpty()).build());
                    }
                }
                case PrimitiveOriginalType primitiveOriginalType ->
                        statements.add(builder.add("$L$L", prettyName, primitiveOriginalType.isEmpty()).build());
                case ObjectOriginalType _ -> {
                    if (node.getNodeType().isAnonymous()) {
                        var codeBlocks = processIsEmptyReturnStatements(node.nodes());
                        statements.addAll(codeBlocks);
                    } else {
                        statements.add(builder.add("$L.isEmpty()", prettyName).build());
                    }
                }
                default -> messager.printMessage(Diagnostic.Kind.ERROR, "unsupported type " + type);
            }
        }
        return statements;
    }
}
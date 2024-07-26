package io.github.digitalsmile.composers;

import com.squareup.javapoet.*;
import io.github.digitalsmile.PackageName;
import io.github.digitalsmile.PrettyName;
import io.github.digitalsmile.annotation.structure.NativeMemoryLayout;
import io.github.digitalsmile.headers.mapping.ArrayOriginalType;
import io.github.digitalsmile.headers.mapping.ObjectOriginalType;
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
                .addJavadoc("Source: $L\n", node.getPosition())
                .addJavadoc("Documentation:\n")
                .addJavadoc(node.getPosition().comment())
                .addFields(processConstructorParameters(node.nodes()))
                .addField(FieldSpec.builder(TypeName.get(MemoryLayout.class), "LAYOUT", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer(
                                CodeBlock.builder()
                                        .add("$T.$LLayout(\n\t", MemoryLayout.class, isUnion ? "union" : "struct")
                                        .add(CodeBlock.join(processMemoryLayout(node.nodes()), ",\n\t"))
                                        .add("\n)")
                                        .build())
                        .build())
                .addFields(processHandleFields(node.nodes()))
                .addMethod(MethodSpec.methodBuilder("createEmpty")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(ClassName.get(packageName, prettyName))
                        .addStatement("return new $N($L)", prettyName, CodeBlock.join(processEmptyConstructor(node.nodes()), ", ")).build())
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
                        .addCode(CodeBlock.join(processFromBodyStatements(node.nodes()), ""))
                        .addCode("return new $L(\n\t", ClassName.get(packageName, prettyName).simpleName())
                        .addCode(CodeBlock.join(processFromReturnStatements(node.nodes(), node), ",\n\t"))
                        .addCode(");")
                        .build())
                .addMethod(MethodSpec.methodBuilder("toBytes")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(AnnotationSpec.builder(Override.class).build())
                        .addParameter(TypeName.get(MemorySegment.class), "buffer")
                        .addException(TypeName.get(Throwable.class))
                        .returns(TypeName.VOID)
                        .addCode(CodeBlock.join(processToBodyStatements(node.nodes(), NodeType.VARIABLE), ""))
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

    private List<FieldSpec> processConstructorParameters(List<NativeMemoryNode> nodes) {
        List<FieldSpec> fieldSpecs = new ArrayList<>();
        for (NativeMemoryNode node : nodes) {
            var type = node.getType();
            switch (type) {
                case ArrayOriginalType arrayType -> {
                    if (arrayType.isObjectType()) {
                        fieldSpecs.add(FieldSpec.builder(ClassName.get("", PrettyName.getObjectName(arrayType.typeName()) + "[]"), PrettyName.getVariableName(node.getName())).build());
                    } else {
                        fieldSpecs.add(FieldSpec.builder(arrayType.carrierClass().arrayType(), PrettyName.getVariableName(node.getName())).build());
                    }
                }
                case PrimitiveOriginalType primitiveType ->
                        fieldSpecs.add(FieldSpec.builder(primitiveType.carrierClass(), PrettyName.getVariableName(node.getName())).build());
                case ObjectOriginalType objectType -> {
                    if (node.getNodeType().isAnonymous()) {
                        fieldSpecs.addAll(processConstructorParameters(node.nodes()));
                    } else {
                        fieldSpecs.add(FieldSpec.builder(ClassName.get("", PrettyName.getObjectName(objectType.typeName())), PrettyName.getVariableName(node.getName())).build());
                    }
                }
                default -> messager.printMessage(Diagnostic.Kind.ERROR, "unsupported type " + type);
            }
        }
        return fieldSpecs;
    }

    private List<CodeBlock> processMemoryLayout(List<NativeMemoryNode> nodes) {
        List<CodeBlock> memoryLayouts = new ArrayList<>();
        for (NativeMemoryNode node : nodes) {
            var type = node.getType();
            var comment = node.getPosition().comment();
            if (!comment.isEmpty()) {
                var comments = comment.split("\n");
                for (var comm : comments) {
                    memoryLayouts.add(CodeBlock.builder().add("// $L", comm).build());
                }
            }
            switch (type) {
                case ArrayOriginalType arrayType -> {
                    if (arrayType.isObjectType()) {
                        memoryLayouts.add(CodeBlock.builder().add("$T.sequenceLayout($L, $L.LAYOUT).withName($S)", MemoryLayout.class,
                                        arrayType.arraySize(), PrettyName.getObjectName(arrayType.typeName()), node.getName())
                                .build());
                    } else {
                        memoryLayouts.add(CodeBlock.builder().add("$T.sequenceLayout($L, $T.$L).withName($S)", MemoryLayout.class,
                                        arrayType.arraySize(), ValueLayout.class, arrayType.valueLayoutName(), node.getName())
                                .build());
                    }
                }
                case PrimitiveOriginalType primitiveType ->
                        memoryLayouts.add(CodeBlock.builder().add("$T.$L.withName($S)", ValueLayout.class, primitiveType.valueLayoutName(), node.getName()).build());
                case ObjectOriginalType objectType -> {
                    if (node.getNodeType().equals(NodeType.ANON_UNION)) {
                        memoryLayouts.add(
                                CodeBlock.builder()
                                        .add("$T.unionLayout(\n\t\t", MemoryLayout.class)
                                        .add(CodeBlock.join(processMemoryLayout(node.nodes()), ",\n\t\t"))
                                        .add("\n\t).withName($S)", node.getName())
                                        .build());
                    } else {
                        var prettyName = PrettyName.getObjectName(objectType.typeName());
                        var packageName = PackageName.getPackageName(objectType.typeName());
                        memoryLayouts.add(CodeBlock.builder().add("$T.LAYOUT.withName($S)", ClassName.get(packageName, prettyName), node.getName()).build());
                    }
                }
                default -> messager.printMessage(Diagnostic.Kind.ERROR, "unsupported type " + type);
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
                        fieldSpecs.add(FieldSpec.builder(TypeName.get(MethodHandle.class), "MH_" + node.getName().toUpperCase())
                                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                                .initializer("LAYOUT.sliceHandle(groupElement($S))", node.getName())
                                .build());
                case PrimitiveOriginalType _ ->
                        fieldSpecs.add(FieldSpec.builder(TypeName.get(VarHandle.class), "VH_" + node.getName().toUpperCase())
                                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                                .initializer("LAYOUT.varHandle(groupElement($S))", node.getName())
                                .build());
                case ObjectOriginalType _ -> {
                    if (node.getNodeType().isAnonymous()) {
                        var fields = processHandleFields(node.nodes());
                        for (FieldSpec spec : fields) {
                            var initializer = spec.initializer.toString();
                            var prefix = spec.type.toString().equals(VarHandle.class.getName()) ? "VH" : "MH";
                            fieldSpecs.add(FieldSpec.builder(TypeName.get(prefix.equals("VH") ? VarHandle.class : MethodHandle.class),
                                            prefix + spec.name.substring(spec.name.indexOf("_")))
                                    .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                                    .initializer("LAYOUT.select(groupElement($S))$L", node.getName(),
                                            initializer.substring(initializer.indexOf(".")))
                                    .build());
                        }
                    } else {
                        fieldSpecs.add(FieldSpec.builder(TypeName.get(MethodHandle.class), "MH_" + node.getName().toUpperCase())
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

    private List<CodeBlock> processEmptyConstructor(List<NativeMemoryNode> nodes) {
        List<CodeBlock> statements = new ArrayList<>();
        for (NativeMemoryNode node : nodes) {
            var type = node.getType();
            switch (type) {
                case ArrayOriginalType arrayType -> {
                    if (arrayType.isObjectType()) {
                        statements.add(CodeBlock.builder().add("new $L[]{}", PrettyName.getObjectName(type.typeName())).build());
                    } else {
                        statements.add(CodeBlock.builder().add(arrayType.newArrayConstructor()).build());
                    }
                }
                case PrimitiveOriginalType primitiveType ->
                        statements.add(CodeBlock.builder().add(primitiveType.newConstructor()).build());
                case ObjectOriginalType _ -> {
                    if (node.getNodeType().isAnonymous()) {
                        var codeBlocks = processEmptyConstructor(node.nodes());
                        statements.addAll(codeBlocks);
                    } else {
                        statements.add(CodeBlock.builder().add("$L.createEmpty()", PrettyName.getObjectName(type.typeName())).build());
                    }
                }
                default -> messager.printMessage(Diagnostic.Kind.ERROR, "unsupported type " + type);
            }
        }
        return statements;
    }

    private List<CodeBlock> processFromBodyStatements(List<NativeMemoryNode> nodes) {
        List<CodeBlock> statements = new ArrayList<>();
        for (NativeMemoryNode node : nodes) {
            var type = node.getType();
            var prettyVariableName = PrettyName.getVariableName(node.getName());
            switch (type) {
                case ArrayOriginalType arrayType -> {
                    if (arrayType.isObjectType()) {
                        statements.add(CodeBlock.builder()
                                .addStatement("var $LMemorySegment = invokeExact(MH_$L, buffer)", prettyVariableName, node.getName().toUpperCase())
                                .addStatement("var $L = new $L[$L]", prettyVariableName, PrettyName.getObjectName(arrayType.typeName()), arrayType.arraySize())
                                .beginControlFlow("for(int i = 0; i < $L; i++)", arrayType.arraySize())
                                .addStatement("var tmp = $L.createEmpty()", PrettyName.getObjectName(arrayType.typeName()))
                                .addStatement("$L[i] = tmp.fromBytes($LMemorySegment.asSlice($L.LAYOUT.byteSize() * i, $L.LAYOUT.byteSize()))",
                                        prettyVariableName, prettyVariableName, PrettyName.getObjectName(arrayType.typeName()), PrettyName.getObjectName(arrayType.typeName()))
                                .endControlFlow()
                                .build());
                    }
                }
                case PrimitiveOriginalType _ -> {
                }
                case ObjectOriginalType objectType -> {
                    if (node.getNodeType().isAnonymous()) {
                        statements.add(CodeBlock.builder()
                                .addStatement("var $LSize = LAYOUT.select($T.groupElement($S)).byteSize()", prettyVariableName, MemoryLayout.PathElement.class, node.getName())
                                .addStatement("var $LBuffer = buffer.asSlice(LAYOUT.byteSize() - $LSize, $LSize)", prettyVariableName, prettyVariableName, prettyVariableName)
                                .build());

                        for (NativeMemoryNode innerNode : node.nodes()) {
                            if (innerNode.getType() instanceof ObjectOriginalType) {
                                var prettyInnerName = PrettyName.getVariableName(innerNode.getName());
                                statements.add(CodeBlock.builder()
                                        .addStatement("var $LMemorySegment = invokeExact(MH_$L, $LBuffer)", prettyInnerName, innerNode.getName().toUpperCase(), prettyVariableName)
                                        .addStatement("var $L = $L.createEmpty().fromBytes($LMemorySegment)", prettyInnerName,
                                                PrettyName.getObjectName(innerNode.getType().typeName()), prettyInnerName)
                                        .build());
                            }
                        }
                    } else {
                        statements.add(CodeBlock.builder()
                                .addStatement("var $LMemorySegment = invokeExact(MH_$L, buffer)", prettyVariableName, node.getName().toUpperCase())
                                .addStatement("var $L = $L.createEmpty().fromBytes($LMemorySegment)", prettyVariableName, PrettyName.getObjectName(objectType.typeName()), prettyVariableName)
                                .build());
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
                    CodeBlock.builder().add("buffer").build();
            switch (type) {
                case ArrayOriginalType arrayType -> {
                    if (arrayType.isObjectType()) {
                        statements.add(CodeBlock.builder().add("$L", PrettyName.getVariableName(node.getName())).build());
                    } else {
                        statements.add(CodeBlock.builder().add("invokeExact(MH_$L, $L).toArray($T.$L)", node.getName().toUpperCase(), bufferName,
                                        ValueLayout.class, arrayType.valueLayoutName())
                                .build());
                    }
                }
                case PrimitiveOriginalType primitiveOriginalType ->
                        statements.add(CodeBlock.builder().add("($T) VH_$L.get($L, 0L)", primitiveOriginalType.carrierClass(), node.getName().toUpperCase(), bufferName).build());
                case ObjectOriginalType _ -> {
                    if (node.getNodeType().isAnonymous()) {
                        var codeBlocks = processFromReturnStatements(node.nodes(), node);
                        statements.addAll(codeBlocks);
                    } else {
                        statements.add(CodeBlock.builder().add("$L", PrettyName.getVariableName(node.getName())).build());
                    }
                }
                default -> messager.printMessage(Diagnostic.Kind.ERROR, "unsupported type " + type);
            }
        }
        return statements;
    }

    private List<CodeBlock> processToBodyStatements(List<NativeMemoryNode> nodes, NodeType parentNodeType) {
        List<CodeBlock> statements = new ArrayList<>();
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
                        builder.addStatement("var $LTmp = invokeExact(MH_$L, buffer)", prettyVariableName, node.getName().toUpperCase())
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
                        builder.addStatement("var $LTmp = invokeExact(MH_$L, buffer)", prettyVariableName, node.getName().toUpperCase())
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
                            CodeBlock.builder().add("buffer").build();
                    builder.addStatement("VH_$L.set($L, 0L, $L)", node.getName().toUpperCase(), bufferName, prettyVariableName);
                    if (parentNodeType.isAnonymous()) {
                        builder.add(CodeBlock.builder().endControlFlow().build());
                    }
                    statements.add(builder.build());
                }
                case ObjectOriginalType _ -> {
                    if (node.getNodeType().isAnonymous()) {
                        var codeBlocks = processToBodyStatements(node.nodes(), node.getNodeType());
                        statements.addAll(codeBlocks);
                    } else {
                        if (parentNodeType.isAnonymous()) {
                            var builder = CodeBlock.builder();
                            builder.add(CodeBlock.builder().beginControlFlow("if (!$L.isEmpty())", prettyVariableName).build());
                            builder.addStatement("var $LTmp = invokeExact(MH_$L, buffer)", prettyVariableName, node.getName().toUpperCase())
                                    .addStatement("$L.toBytes($LTmp)", prettyVariableName, prettyVariableName);
                            builder.add(CodeBlock.builder().endControlFlow().build());
                            statements.add(builder.build());
                        } else {
                            var builder = CodeBlock.builder();
                            builder.addStatement("var $LTmp = invokeExact(MH_$L, buffer)", prettyVariableName, node.getName().toUpperCase())
                                    .addStatement("$L.toBytes($LTmp)", prettyVariableName, prettyVariableName);
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
            switch (type) {
                case ArrayOriginalType arrayType -> {
                    if (arrayType.isObjectType()) {
                        statements.add(CodeBlock.builder().add("$L.length > 0", PrettyName.getVariableName(node.getName())).build());
                    } else {
                        statements.add(CodeBlock.builder().add("$L$L", PrettyName.getVariableName(node.getName()), arrayType.isArrayEmpty()).build());
                    }
                }
                case PrimitiveOriginalType primitiveOriginalType ->
                        statements.add(CodeBlock.builder().add("$L$L", PrettyName.getVariableName(node.getName()), primitiveOriginalType.isEmpty()).build());
                case ObjectOriginalType _ -> {
                    if (node.getNodeType().isAnonymous()) {
                        var codeBlocks = processIsEmptyReturnStatements(node.nodes());
                        statements.addAll(codeBlocks);
                    } else {
                        statements.add(CodeBlock.builder().add("$L.isEmpty()", PrettyName.getVariableName(node.getName())).build());
                    }
                }
                default -> messager.printMessage(Diagnostic.Kind.ERROR, "unsupported type " + type);
            }
        }
        return statements;
    }
}
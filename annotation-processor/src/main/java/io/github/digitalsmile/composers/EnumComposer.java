package io.github.digitalsmile.composers;

import com.squareup.javapoet.*;
import io.github.digitalsmile.PackageName;
import io.github.digitalsmile.annotation.structure.NativeMemoryLayout;
import io.github.digitalsmile.headers.mapping.ObjectOriginalType;
import io.github.digitalsmile.headers.model.NativeMemoryNode;
import io.github.digitalsmile.headers.mapping.PrimitiveOriginalType;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Modifier;
import javax.tools.Diagnostic;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Arrays;

public class EnumComposer {
    private final Messager messager;

    public EnumComposer(Messager messager) {
        this.messager = messager;
    }

    public String compose(String prettyName, NativeMemoryNode node) {
        var packageName = PackageName.getPackageName(node.getName());
        var sameTypes = node.nodes().stream()
                .map(p -> p.getType().typeName())
                .allMatch(node.nodes().getFirst().getType().typeName()::equals);
        if (!sameTypes) {
            var classBuilder = TypeSpec.classBuilder(prettyName)
                    .addModifiers(Modifier.PUBLIC)
                    .addJavadoc("Source: $L\n", node.getPosition())
                    .addJavadoc("Documentation:\n")
                    .addJavadoc(node.getPosition().comment());
            for (NativeMemoryNode internalNode : node.nodes()) {
                if (internalNode.getType() instanceof PrimitiveOriginalType type) {
                    var checkedType = checkValue(type, internalNode.getValue());
                    var fieldSpec = FieldSpec.builder(checkedType.valueLayout().carrier(), internalNode.getName(), Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                            .initializer("$L$L", internalNode.getValue(), checkedType.literal())
                            .addJavadoc("Source: $L\n", internalNode.getPosition())
                            .addJavadoc("Documentation:\n")
                            .addJavadoc(internalNode.getPosition().comment())
                            .build();
                    classBuilder.addField(fieldSpec);
                } else if (internalNode.getType() instanceof ObjectOriginalType type) {
                    if (type.carrierClass().equals(String.class)) {
                        var fieldSpec = FieldSpec.builder(type.carrierClass(), internalNode.getName(), Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                                .initializer("$S", internalNode.getValue())
                                .addJavadoc("Source: $L\n", internalNode.getPosition())
                                .addJavadoc("Documentation:\n")
                                .addJavadoc(internalNode.getPosition().comment())
                                .build();
                        classBuilder.addField(fieldSpec);
                    }
                }
            }
            var outputFile = JavaFile.builder(packageName, classBuilder.build()).indent("\t").skipJavaLangImports(true).build();
            return outputFile.toString();
        } else {
            var type = (PrimitiveOriginalType) node.nodes().getFirst().getType();
            var enumBuilder = TypeSpec.enumBuilder(prettyName)
                    .addModifiers(Modifier.PUBLIC)
                    .addJavadoc("Source: $L\n", node.getPosition())
                    .addJavadoc("Documentation:\n")
                    .addJavadoc(node.getPosition().comment())
                    .addSuperinterface(NativeMemoryLayout.class)
                    .addField(FieldSpec.builder(MemoryLayout.class, "LAYOUT", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                            .initializer("$T.$L", ValueLayout.class, type.valueLayoutName())
                            .build())
                    .addMethod(MethodSpec.methodBuilder("createEmpty").addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                            .returns(ClassName.get(packageName, prettyName))
                            .addStatement("return null")
                            .build())
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
                            .addStatement("var value = buffer.get($T.$L, 0)", ValueLayout.class, type.valueLayoutName())
                            .addStatement("return $T.stream(values()).filter(p -> p.getValue() == value).findFirst().orElseThrow()", Arrays.class)
                            .build())
                    .addMethod(MethodSpec.methodBuilder("toBytes")
                            .addModifiers(Modifier.PUBLIC)
                            .addAnnotation(AnnotationSpec.builder(Override.class).build())
                            .addParameter(TypeName.get(MemorySegment.class), "buffer")
                            .addException(TypeName.get(Throwable.class))
                            .returns(TypeName.VOID)
                            .addStatement("buffer.set($T.$L, 0, getValue())", ValueLayout.class, type.valueLayoutName())
                            .build())
                    .addMethod(MethodSpec.methodBuilder("isEmpty")
                            .addModifiers(Modifier.PUBLIC)
                            .addAnnotation(AnnotationSpec.builder(Override.class).build())
                            .returns(TypeName.BOOLEAN.unbox())
                            .addStatement("return false")
                            .build());
            for (NativeMemoryNode internalNode : node.nodes()) {
                enumBuilder.addEnumConstant(internalNode.getName(),
                        TypeSpec.anonymousClassBuilder("$L", internalNode.getValue())
                                .addJavadoc("Source: $L\n", internalNode.getPosition())
                                .addJavadoc("Documentation:\n")
                                .addJavadoc(internalNode.getPosition().comment())
                                .build());
            }
            var outputFile = JavaFile.builder(packageName,
                    enumBuilder
                            .addField(type.valueLayout().carrier(), "value", Modifier.PRIVATE, Modifier.FINAL)
                            .addMethod(MethodSpec.constructorBuilder()
                                    .addParameter(type.valueLayout().carrier(), "value")
                                    .addStatement("this.$N = $N", "value", "value")
                                    .build())
                            .addMethod(MethodSpec.methodBuilder("getValue").addModifiers(Modifier.PUBLIC)
                                    .returns(type.carrierClass())
                                    .addStatement("return value")
                                    .build())
                            .build()
            ).indent("\t").skipJavaLangImports(true).build();
            return outputFile.toString();
        }
    }

    private PrimitiveOriginalType checkValue(PrimitiveOriginalType type, Object value) {
        switch (type.valueLayout()) {
            case ValueLayout.OfInt _ -> {
                var valueToCheck = (long) value;
                if (valueToCheck > Integer.MAX_VALUE || valueToCheck < Integer.MIN_VALUE) {
                    return new PrimitiveOriginalType(type.typeName(), ValueLayout.JAVA_LONG);
                }
            }
            case ValueLayout.OfShort _ -> {
                var valueToCheck = (int) value;
                if (valueToCheck > Short.MAX_VALUE || valueToCheck < Short.MIN_VALUE) {
                    return new PrimitiveOriginalType(type.typeName(), ValueLayout.JAVA_INT);
                }
            }
            default -> {
                return type;
            }
        }
        return type;
    }
}

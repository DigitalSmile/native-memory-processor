package io.github.digitalsmile.composers;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import io.github.digitalsmile.headers.mapping.PrimitiveTypeMapping;
import io.github.digitalsmile.headers.model.NativeMemoryNode;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Modifier;

public class EnumComposer {
    private final Messager messager;

    public EnumComposer(Messager messager) {
        this.messager = messager;
    }

    public String compose(String packageName, String prettyName, NativeMemoryNode parentNode) {
        var sameTypes = parentNode.nodes().stream()
                .map(p -> p.getType().typeName())
                .allMatch(parentNode.nodes().getFirst().getType().typeName()::equals);
        if (!sameTypes) {
            var classBuilder = TypeSpec.classBuilder(prettyName)
                    .addModifiers(Modifier.PUBLIC)
                    .addJavadoc("Source: $L", parentNode.getSource());
            for (NativeMemoryNode node : parentNode.nodes()) {
                var type = (PrimitiveTypeMapping) node.getType().typeMapping();
                var fieldSpec = FieldSpec.builder(type.valueLayout().carrier(), node.getName(), Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer("$L$L", node.getValue(), type.literal())
                        .addJavadoc("Source: $L", node.getSource())
                        .build();
                classBuilder.addField(fieldSpec);
            }
            var outputFile = JavaFile.builder(packageName, classBuilder.build()).indent("\t").skipJavaLangImports(true).build();
            return outputFile.toString();
        } else {
            var type = (PrimitiveTypeMapping) parentNode.nodes().getFirst().getType().typeMapping();
            var enumBuilder = TypeSpec.enumBuilder(prettyName)
                    .addModifiers(Modifier.PUBLIC)
                    .addJavadoc("Source: $L", parentNode.getSource());
            for (NativeMemoryNode node : parentNode.nodes()) {
                enumBuilder.addEnumConstant(node.getName(),
                        TypeSpec.anonymousClassBuilder("$L", node.getValue())
                                .addJavadoc("Source: $L", node.getSource())
                                .build());
            }
            var outputFile = JavaFile.builder(packageName,
                    enumBuilder
                            .addField(type.valueLayout().carrier(), "value", Modifier.PRIVATE, Modifier.FINAL)
                            .addMethod(MethodSpec.constructorBuilder()
                                    .addParameter(type.valueLayout().carrier(), "value")
                                    .addStatement("this.$N = $N", "value", "value")
                                    .build())
                            .build()
            ).indent("\t").skipJavaLangImports(true).build();
            return outputFile.toString();
        }
    }
}

package io.github.digitalsmile.composers;

import com.squareup.javapoet.*;
import io.github.digitalsmile.PackageName;
import io.github.digitalsmile.annotation.structure.NativeMemoryLayout;
import io.github.digitalsmile.headers.model.NativeMemoryNode;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Modifier;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

public class OpaqueComposer {

    private final Messager messager;

    public OpaqueComposer(Messager messager) {
        this.messager = messager;
    }

    public String compose(String prettyName, NativeMemoryNode node) {
        var packageName = PackageName.getPackageName(node.getName());
        var record = TypeSpec.classBuilder(prettyName)
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(TypeName.get(NativeMemoryLayout.class))
                .addJavadoc("Source: $L\n", node.getPosition())
                .addJavadoc("Documentation:\n")
                .addJavadoc(node.getPosition().comment())
                .addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE)
                        .addParameter(MemorySegment.class, "memorySegment")
                        .addStatement(CodeBlock.builder().add("this.memorySegment = memorySegment").build())
                        .build())
                .addField(FieldSpec.builder(TypeName.get(MemoryLayout.class), "LAYOUT", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer(
                                CodeBlock.builder()
                                        .add("$T.ADDRESS", ValueLayout.class)
                                        .build())
                        .build())
                .addField(FieldSpec.builder(TypeName.get(MemorySegment.class), "memorySegment", Modifier.PRIVATE).build())
                .addMethod(MethodSpec.methodBuilder("create")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .addParameter(MemorySegment.class, "memorySegment")
                        .returns(ClassName.get(packageName, prettyName))
                        .addStatement("return new $N(memorySegment)", prettyName).build())
                .addMethod(MethodSpec.methodBuilder("createEmpty")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(ClassName.get(packageName, prettyName))
                        .addStatement("return new $N(null)", prettyName).build())
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
                        .addStatement("return new $L(buffer)", ClassName.get(packageName, prettyName).simpleName())
                        .build())
                .addMethod(MethodSpec.methodBuilder("toBytes")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(AnnotationSpec.builder(Override.class).build())
                        .addParameter(TypeName.get(MemorySegment.class), "buffer")
                        .addException(TypeName.get(Throwable.class))
                        .returns(TypeName.VOID)
                        .addCode(CodeBlock.builder().addStatement("this.memorySegment = buffer").build())
                        .build())
                .addMethod(MethodSpec.methodBuilder("isEmpty")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(AnnotationSpec.builder(Override.class).build())
                        .returns(TypeName.BOOLEAN.unbox())
                        .addCode(CodeBlock.builder().addStatement("return memorySegment != null").build())
                        .build())
                .build();
        var outputFile = JavaFile.builder(packageName, record)
                .indent("\t").skipJavaLangImports(true).build();
        return outputFile.toString();

    }

}

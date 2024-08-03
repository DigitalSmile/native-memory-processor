package io.github.digitalsmile.composers;

import com.squareup.javapoet.*;
import io.github.digitalsmile.PackageName;
import io.github.digitalsmile.annotation.types.interfaces.OpaqueMemoryLayout;
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
        var record = TypeSpec.recordBuilder(prettyName)
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(TypeName.get(OpaqueMemoryLayout.class))
                .addJavadoc("Source: $L$L", node.getPosition(), node.getPosition().comment().isEmpty() ? "" : "\n\n")
                .addJavadoc(node.getPosition().comment())
                .addField(FieldSpec.builder(MemorySegment.class, "memorySegment")
                        .build())
                .addField(FieldSpec.builder(TypeName.get(MemoryLayout.class), "LAYOUT", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer(
                                CodeBlock.builder()
                                        .add("$T.ADDRESS", ValueLayout.class)
                                        .build())
                        .build())
                .addMethod(MethodSpec.methodBuilder("create")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .addParameter(MemorySegment.class, "memorySegment")
                        .returns(ClassName.get(packageName, prettyName))
                        .addStatement("return new $N(memorySegment)", prettyName).build())
                .addMethod(MethodSpec.methodBuilder("createEmpty")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(ClassName.get(packageName, prettyName))
                        .addStatement("return new $N($T.NULL)", prettyName, MemorySegment.class).build())
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
                        .addCode(CodeBlock.builder().addStatement("memorySegment().copyFrom(buffer)").build())
                        .build())
                .addMethod(MethodSpec.methodBuilder("isEmpty")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(AnnotationSpec.builder(Override.class).build())
                        .returns(TypeName.BOOLEAN.unbox())
                        .addCode(CodeBlock.builder().addStatement("return memorySegment.equals($T.NULL)", MemorySegment.class).build())
                        .build())
                .build();
        var outputFile = JavaFile.builder(packageName, record)
                .indent("\t").skipJavaLangImports(true).build();
        return outputFile.toString();

    }

}

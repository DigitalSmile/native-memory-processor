package io.github.digitalsmile.type;

import com.squareup.javapoet.CodeBlock;
import org.openjdk.jextract.Type;

import java.util.List;

public record ObjectTypeMapping(Class<?> type) implements TypeMapping {


    @Override
    public Class<?> carrierClass() {
        return type;
    }

    @Override
    public Class<?> arrayClass() {
        return type.arrayType();
    }

    @Override
    public CodeBlock valueLayoutName() {
        return CodeBlock.builder().add("ADDRESS").build();
    }

    @Override
    public CodeBlock newConstructor() {
        return null;
    }

    @Override
    public CodeBlock newArrayConstructor() {
        return null;
    }

    @Override
    public CodeBlock isEmpty() {
        return null;
    }

    @Override
    public CodeBlock isNotEmpty() {
        return null;
    }

    @Override
    public CodeBlock isNotArrayEmpty() {
        return null;
    }

    @Override
    public CodeBlock isArrayEmpty() {
        return null;
    }

    @Override
    public List<Type.Primitive.Kind> types() {
        return List.of();
    }
}

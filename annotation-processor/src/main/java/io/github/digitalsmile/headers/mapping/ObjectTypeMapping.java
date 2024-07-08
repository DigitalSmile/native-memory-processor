package io.github.digitalsmile.headers.mapping;

import com.squareup.javapoet.CodeBlock;

public record ObjectTypeMapping(String typeName, Class<?> clazz, boolean isGenerated) implements TypeMapping {
    public ObjectTypeMapping(String typeName) {
        this(typeName, Object.class, true);
    }

    public ObjectTypeMapping(Class<?> clazz) {
        this(clazz.getSimpleName(), clazz, false);
    }

    @Override
    public Class<?> carrierClass() {
        return clazz;
    }

    @Override
    public CodeBlock valueLayoutName() {
        return CodeBlock.builder().add("$L", "ADDRESS").build();
    }
}

package io.github.digitalsmile.headers.mapping;

import com.squareup.javapoet.CodeBlock;

public record DeferredObjectTypeMapping(String typeName) implements TypeMapping {
    @Override
    public Class<?> carrierClass() {
        return Object.class;
    }

    @Override
    public CodeBlock valueLayoutName() {
        return CodeBlock.builder().add("$L", "ADDRESS").build();
    }
}

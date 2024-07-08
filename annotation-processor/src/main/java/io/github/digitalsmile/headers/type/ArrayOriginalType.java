package io.github.digitalsmile.headers.type;

import io.github.digitalsmile.headers.mapping.TypeMapping;

public record ArrayOriginalType(String typeName, long arraySize, TypeMapping typeMapping) implements OriginalType {
    @Override
    public String toString() {
        return typeMapping.typeName() + "[" + arraySize + "]";
    }
}

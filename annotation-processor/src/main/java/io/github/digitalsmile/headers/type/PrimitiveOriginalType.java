package io.github.digitalsmile.headers.type;

import io.github.digitalsmile.headers.mapping.PrimitiveTypeMapping;

public record PrimitiveOriginalType(String typeName, PrimitiveTypeMapping typeMapping) implements OriginalType {
    @Override
    public String toString() {
        return typeMapping.typeName();
    }
}

package io.github.digitalsmile.headers.type;

import io.github.digitalsmile.headers.mapping.TypeMapping;

public record ObjectOriginalType(String typeName, TypeMapping typeMapping) implements OriginalType {
    @Override
    public String toString() {
        return typeName;
    }
}

package io.github.digitalsmile.headers.mapping;

import io.github.digitalsmile.annotation.structure.NativeMemoryLayout;

import java.lang.foreign.ValueLayout;

public record ObjectOriginalType(String typeName) implements OriginalType {

    @Override
    public Class<?> carrierClass() {
        return switch (typeName) {
            case "String" -> String.class;
            case "void" -> void.class;
            case "NativeMemoryLayout" -> NativeMemoryLayout.class;
            default -> Object.class;
        };
    }

    @Override
    public ValueLayout valueLayout() {
        return ValueLayout.ADDRESS;
    }

    @Override
    public String toString() {
        return typeName;
    }
}

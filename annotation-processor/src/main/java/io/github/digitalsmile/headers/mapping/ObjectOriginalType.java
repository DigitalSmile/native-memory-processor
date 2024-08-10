package io.github.digitalsmile.headers.mapping;

import io.github.digitalsmile.annotation.types.RawPointer;

import java.lang.foreign.ValueLayout;

public record ObjectOriginalType(String typeName, long alignment) implements OriginalType {

    public ObjectOriginalType(String typeName) {
        this(typeName, ValueLayout.ADDRESS.byteAlignment());
    }

    @Override
    public Class<?> carrierClass() {
        try {
            return switch (typeName()) {
                case "String" -> String.class;
                case "void" -> void.class;
                default -> Class.forName(typeName());
            };
        } catch (ClassNotFoundException e) {
            return Object.class;
        }

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

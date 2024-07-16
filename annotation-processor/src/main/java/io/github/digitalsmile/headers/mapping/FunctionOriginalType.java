package io.github.digitalsmile.headers.mapping;

import java.lang.foreign.ValueLayout;

public record FunctionOriginalType() implements OriginalType {

    @Override
    public String typeName() {
        return "Unsupported";
    }

    @Override
    public Class<?> carrierClass() {
        return void.class;
    }

    @Override
    public ValueLayout valueLayout() {
        return ValueLayout.ADDRESS;
    }
}

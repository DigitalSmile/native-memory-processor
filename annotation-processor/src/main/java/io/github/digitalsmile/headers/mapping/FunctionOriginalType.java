package io.github.digitalsmile.headers.mapping;

import java.lang.foreign.ValueLayout;
import java.util.List;

public record FunctionOriginalType(OriginalType returns, List<OriginalType> parameterTypes, List<String> parameterNames) implements OriginalType {

    @Override
    public String typeName() {
        return returns.typeName();
    }

    @Override
    public Class<?> carrierClass() {
        return returns.carrierClass();
    }

    @Override
    public ValueLayout valueLayout() {
        return returns.valueLayout();
    }

    @Override
    public String toString() {
        return "(" + parameterTypes + ") (" + parameterNames + ") (returns: " + returns.typeName() + ")";
    }
}

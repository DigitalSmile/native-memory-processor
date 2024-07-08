package io.github.digitalsmile.headers.type;

import io.github.digitalsmile.headers.mapping.TypeMapping;

public record FunctionOriginalType() implements OriginalType {

    @Override
    public String typeName() {
        return "Unsupported";
    }

    @Override
    public <T extends TypeMapping> T typeMapping() {
        return null;
    }
}

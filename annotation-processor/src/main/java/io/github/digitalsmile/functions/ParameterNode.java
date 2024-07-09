package io.github.digitalsmile.functions;

import io.github.digitalsmile.headers.type.OriginalType;

public record ParameterNode(String name, OriginalType typeMapping, boolean returns, boolean byAddress) {

    @Override
    public String toString() {
        return "ParameterNode{" +
                "name='" + name + '\'' +
                ", typeMapping=" + typeMapping +
                ", returns=" + returns +
                ", byAddress=" + byAddress +
                '}';
    }
}

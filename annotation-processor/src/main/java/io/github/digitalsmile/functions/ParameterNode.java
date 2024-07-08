package io.github.digitalsmile.functions;

import io.github.digitalsmile.headers.type.OriginalType;

public class ParameterNode {
    private final String name;
    private final OriginalType typeMapping;
    private final boolean returns;
    private final boolean byAddress;

    public ParameterNode(String name, OriginalType typeMapping, boolean returns, boolean byAddress) {
        this.name = name;
        this.typeMapping = typeMapping;
        this.returns = returns;
        this.byAddress = byAddress;
    }

    public String getName() {
        return name;
    }

    public OriginalType getTypeMapping() {
        return typeMapping;
    }

    public boolean isReturns() {
        return returns;
    }

    public boolean isByAddress() {
        return byAddress;
    }

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

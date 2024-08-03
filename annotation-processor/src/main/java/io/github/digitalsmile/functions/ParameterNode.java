package io.github.digitalsmile.functions;

import io.github.digitalsmile.headers.mapping.OriginalType;
import io.github.digitalsmile.headers.model.NativeMemoryNode;

import javax.lang.model.type.TypeMirror;

public record ParameterNode(String name, NativeMemoryNode nativeMemoryNode, boolean returns, boolean byAddress) {

    @Override
    public String toString() {
        return "ParameterNode{" +
                "name='" + name + '\'' +
                ", returns=" + returns +
                ", byAddress=" + byAddress +
                '}';
    }
}

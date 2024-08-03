package io.github.digitalsmile.functions;

import io.github.digitalsmile.headers.model.NativeMemoryNode;

public record ParameterNode(String name, NativeMemoryNode nativeMemoryNode, boolean returns, boolean byAddress) {
}

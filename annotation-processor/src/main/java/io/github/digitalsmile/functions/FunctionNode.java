package io.github.digitalsmile.functions;

import io.github.digitalsmile.headers.model.NativeMemoryNode;

import javax.lang.model.element.TypeParameterElement;
import java.util.List;

public record FunctionNode(String functionName, FunctionOptions functionOptions, NativeMemoryNode returnNode,
                           List<ParameterNode> functionParameters, List<? extends TypeParameterElement> typeVariables) {
}

package io.github.digitalsmile.functions;

import io.github.digitalsmile.headers.mapping.OriginalType;

import java.util.List;

public record FunctionNode(String functionName, OriginalType nativeReturnType, OriginalType returnType,
                           List<ParameterNode> functionParameters, boolean useErrno) {

    @Override
    public String toString() {
        return "FunctionNode{" +
                "functionName='" + functionName + '\'' +
                ", nativeReturnType=" + nativeReturnType +
                ", returnType=" + returnType +
                ", functionParameters=" + functionParameters +
                ", useErrno=" + useErrno +
                '}';
    }
}

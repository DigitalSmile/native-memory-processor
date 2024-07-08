package io.github.digitalsmile.functions;

import io.github.digitalsmile.headers.type.OriginalType;

import java.util.List;

public class FunctionNode {
    private final String functionName;
    private final OriginalType nativeReturnType;
    private final OriginalType returnType;
    private final List<ParameterNode> functionParameters;
    private final boolean useErrno;

    public FunctionNode(String functionName, OriginalType nativeReturnType, OriginalType returnType, List<ParameterNode> functionParameters, boolean useErrno) {
        this.functionName = functionName;
        this.nativeReturnType = nativeReturnType;
        this.returnType = returnType;
        this.functionParameters = functionParameters;
        this.useErrno = useErrno;
    }

    public String getFunctionName() {
        return functionName;
    }

    public OriginalType getNativeReturnType() {
        return nativeReturnType;
    }

    public OriginalType getReturnType() {
        return returnType;
    }

    public List<ParameterNode> getFunctionParameters() {
        return functionParameters;
    }

    public boolean isUseErrno() {
        return useErrno;
    }

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

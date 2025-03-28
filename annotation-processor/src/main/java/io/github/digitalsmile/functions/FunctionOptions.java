package io.github.digitalsmile.functions;

import io.github.digitalsmile.headers.mapping.OriginalType;

public record FunctionOptions(String nativeFunctionName, boolean isAlreadyLoaded, boolean useErrno, OriginalType nativeReturnType) {
}

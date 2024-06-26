package io.github.digitalsmile;

import io.github.digitalsmile.annotation.NativeMemory;
import io.github.digitalsmile.annotation.function.Function;
import io.github.digitalsmile.annotation.function.NativeMemoryException;

@NativeMemory
public interface Pid {
    @Function(name = "getpid", returnType = int.class)
    int nativeCall() throws NativeMemoryException;
}
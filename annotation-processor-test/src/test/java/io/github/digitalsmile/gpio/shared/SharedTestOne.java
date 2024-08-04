package io.github.digitalsmile.gpio.shared;

import io.github.digitalsmile.annotation.ArenaType;
import io.github.digitalsmile.annotation.NativeMemory;
import io.github.digitalsmile.annotation.NativeMemoryException;
import io.github.digitalsmile.annotation.NativeMemoryOptions;
import io.github.digitalsmile.annotation.function.NativeManualFunction;
import io.github.digitalsmile.annotation.structure.Structs;

@NativeMemory(headers = "shared/test1.h")
@NativeMemoryOptions(arena = ArenaType.CONFINED)
@Structs
public interface SharedTestOne {

    @NativeManualFunction(name = "open64")
    int open(String path, int openFlag) throws NativeMemoryException;
}

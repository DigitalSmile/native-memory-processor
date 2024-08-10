package io.github.digitalsmile.gpio.shared;

import io.github.digitalsmile.annotation.NativeMemory;
import io.github.digitalsmile.annotation.NativeMemoryException;
import io.github.digitalsmile.annotation.NativeMemoryOptions;
import io.github.digitalsmile.annotation.function.NativeManualFunction;
import io.github.digitalsmile.annotation.function.Returns;
import io.github.digitalsmile.annotation.structure.Struct;
import io.github.digitalsmile.annotation.structure.Structs;
import io.github.digitalsmile.gpio.shared.system.Stat;

@NativeMemory(headers = "/usr/include/x86_64-linux-gnu/sys/stat.h")
@NativeMemoryOptions(systemHeader = true)
@Structs(
        @Struct(name = "stat", javaName = "Stat")
)
public interface SharedTestTwo {

    @NativeManualFunction(name = "fstat")
    Stat stat(int fd, @Returns Stat stat) throws NativeMemoryException;
}

package io.github.digitalsmile.gpio.functions;

import io.github.digitalsmile.annotation.NativeMemory;
import io.github.digitalsmile.annotation.NativeMemoryOptions;
import io.github.digitalsmile.annotation.function.*;
import io.github.digitalsmile.annotation.NativeMemoryException;
import io.github.digitalsmile.annotation.structure.Enums;
import io.github.digitalsmile.annotation.structure.Struct;
import io.github.digitalsmile.annotation.structure.Structs;
import io.github.digitalsmile.annotation.structure.Unions;
import io.github.digitalsmile.annotation.types.interfaces.NativeMemoryLayout;
@NativeMemory(headers = "/usr/include/x86_64-linux-gnu/sys/stat.h")
@NativeMemoryOptions(systemIncludes = {
        "/usr/lib/gcc/x86_64-linux-gnu/${gcc-version}/include/"
}, processRootConstants = true, debugMode = true)
@Structs({
        @Struct(name = "stat", javaName = "Stat")
})
@Enums
@Unions
public interface LibcFunctions {
    @NativeManualFunction(name = "ioctl", useErrno = true)
    int callByValue(int fd, long command, long data) throws NativeMemoryException;

    @NativeManualFunction(name = "ioctl")
    long call(int fd, long command, @Returns @ByAddress long data) throws NativeMemoryException;

    @NativeManualFunction(name = "ioctl", useErrno = true)
    int call(int fd, long command, @Returns @ByAddress int data) throws NativeMemoryException;

    @NativeManualFunction(name = "ioctl", useErrno = true)
    <T extends NativeMemoryLayout> T call(int fd, long command, @Returns T data) throws NativeMemoryException;

    @NativeManualFunction(name = "open64", useErrno = true)
    int open(@ByAddress String path, int openFlag) throws NativeMemoryException;

    @NativeManualFunction(name = "close")
    void close(int fd) throws NativeMemoryException;

    @NativeManualFunction(name = "read", useErrno = true, nativeReturnType = int.class)
    byte[] read(int fd, @Returns @ByAddress byte[] buffer, int size) throws NativeMemoryException;

    @NativeManualFunction(name = "write", useErrno = true)
    int write(int fd, @ByAddress byte[] data) throws NativeMemoryException;
}

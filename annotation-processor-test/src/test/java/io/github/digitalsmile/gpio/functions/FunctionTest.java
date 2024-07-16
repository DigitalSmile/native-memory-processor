package io.github.digitalsmile.gpio.functions;

import io.github.digitalsmile.annotation.NativeMemory;
import io.github.digitalsmile.annotation.function.ByAddress;
import io.github.digitalsmile.annotation.function.NativeFunction;
import io.github.digitalsmile.annotation.function.NativeMemoryException;
import io.github.digitalsmile.annotation.function.Returns;
import io.github.digitalsmile.annotation.structure.NativeMemoryLayout;

@NativeMemory
public interface FunctionTest {
        @NativeFunction(name = "ioctl", useErrno = true, returnType = int.class)
        int callByValue(int fd, long command, long data) throws NativeMemoryException;

        @NativeFunction(name = "ioctl", useErrno = true, returnType = int.class)
        long call(int fd, long command, @Returns @ByAddress long data) throws NativeMemoryException;

        @NativeFunction(name = "ioctl", useErrno = true, returnType = int.class)
        int call(int fd, long command, @Returns @ByAddress int data)  throws NativeMemoryException;

        @NativeFunction(name = "ioctl", useErrno = true, returnType = int.class)
        <T extends NativeMemoryLayout> T call(int fd, long command, @Returns T data) throws NativeMemoryException;

        @NativeFunction(name = "open64", useErrno = true, returnType = int.class)
        int open(@ByAddress String path, int openFlag) throws NativeMemoryException;

        @NativeFunction(name = "close", useErrno = true)
        void close(int fd)throws NativeMemoryException;

        @NativeFunction(name = "read", useErrno = true, returnType = int.class)
        byte[] read(int fd, @Returns @ByAddress byte[] buffer, int size) throws NativeMemoryException;

        @NativeFunction(name = "write", useErrno = true, returnType = int.class)
        int write(int fd, @ByAddress byte[] data) throws NativeMemoryException;
}

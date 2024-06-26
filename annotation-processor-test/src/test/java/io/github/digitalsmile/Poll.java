package io.github.digitalsmile;

import io.github.digitalsmile.annotation.NativeMemory;
import io.github.digitalsmile.annotation.function.Function;
import io.github.digitalsmile.annotation.function.NativeMemoryException;
import io.github.digitalsmile.annotation.structure.Struct;
import io.github.digitalsmile.annotation.structure.Structs;

@NativeMemory(header = "poll.h")
@Structs({
        @Struct(name = "pollfd", javaName = "PollingFileDescriptor")
})
public interface Poll {
    @Function(name = "poll", useErrno = true, returnType = int.class)
    int nativeCall(PollingFileDescriptor descriptor, int size, int timeout) throws NativeMemoryException;
}
package io.github.digitalsmile;

import io.github.digitalsmile.annotation.NativeMemory;
import io.github.digitalsmile.annotation.function.Function;
import io.github.digitalsmile.annotation.function.NativeMemoryException;
import io.github.digitalsmile.annotation.function.Returns;
import io.github.digitalsmile.annotation.structure.Struct;
import io.github.digitalsmile.annotation.structure.Structs;

@NativeMemory(header = "/usr/src/linux-headers-6.2.0-39/include/uapi/linux/gpio.h")
@Structs({
        @Struct(name = "gpiochip_info", javaName = "ChipInfo")
})
public interface Ioctl {

    @Function(name = "ioctl", useErrno = true, returnType = int.class)
    ChipInfo nativeCall(int fd, long command, @Returns ChipInfo data) throws NativeMemoryException;
}



package io.github.digitalsmile.gpio.smbus;

import io.github.digitalsmile.annotation.NativeMemory;
import io.github.digitalsmile.annotation.NativeMemoryOptions;
import io.github.digitalsmile.annotation.structure.Struct;
import io.github.digitalsmile.annotation.structure.Structs;

//@NativeMemory(headers = "/home/ds/linux/include/uapi/linux/i2c-dev.h")
@NativeMemoryOptions(
        includes = "/home/ds/linux/include/",
        systemIncludes = {"/home/ds/linux/include/"})
@Structs({
        @Struct(name = "i2c_smbus_ioctl_data", javaName = "SMBusIoctlData")
})
public interface SMBus {
}

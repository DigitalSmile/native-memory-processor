package io.github.digitalsmile.gpio.types.all;

import io.github.digitalsmile.annotation.NativeMemory;
import io.github.digitalsmile.annotation.NativeMemoryOptions;
import io.github.digitalsmile.annotation.structure.Enums;
import io.github.digitalsmile.annotation.structure.Structs;
import io.github.digitalsmile.annotation.structure.Unions;

@NativeMemory(headers = "/usr/src/linux-headers-{version}/include/uapi/linux/gpio.h")
@NativeMemoryOptions(
        processRootConstants = true
)
@Structs
@Enums
@Unions
public interface GPIOTypesAll {
}

//@NativeMemory(headers = "/usr/src/linux-headers-6.2.0-39/include/uapi/linux/gpio.h")
//@NativeMemoryOptions(
//        processRootConstants = true
//)
//@Structs({
//        @Struct(name = "gpiochip_info", javaName = "ChipInfo"),
//        @Struct(name = "gpio_v2_line_attribute", javaName = "LineAttribute"),
//        @Struct(name = "gpio_v2_line_config", javaName = "LineConfig"),
//        @Struct(name = "gpio_v2_line_config_attribute", javaName = "LineConfigAttribute"),
//        @Struct(name = "gpio_v2_line_event", javaName = "LineEvent"),
//        @Struct(name = "gpio_v2_line_info", javaName = "LineInfo"),
//        @Struct(name = "gpio_v2_line_request", javaName = "LineRequest"),
//        @Struct(name = "gpio_v2_line_values", javaName = "LineValues")
//})
//@Enums({
//        @Enum(name = "gpio_v2_line_event_id", javaName = "LineEventId")
//})
////@Enums
//public interface GPIOTypesPrimitives {
//
//    @Function(name = "ioctl", useErrno = true, returnType = int.class)
//    ChipInfo nativeCall(int fd, long command, @Returns ChipInfo data) throws NativeMemoryException;
//
//    @Function(name = "ioctl", useErrno = true, returnType = ChipInfo.class)
//    ChipInfo nativeCall(int fd, double command, @Returns ChipInfo data) throws NativeMemoryException;
//
//    @Function(name = "ioctl", useErrno = true)
//    void nativeCall2(int fd, double command, ChipInfo data) throws NativeMemoryException;
//
//    @Function(name = "ioctl", useErrno = true, returnType = float.class)
//    float nativeCall2(int fd, long command, int data) throws NativeMemoryException;
//
//    @Function(name = "ioctl", useErrno = true, returnType = float.class)
//    float nativeCall2(int fd, long command, float data) throws NativeMemoryException;
//
//    @Function(name = "ioctl", useErrno = true)
//    void nativeCall2(double fd, long command, float data) throws NativeMemoryException;
//}


//@NativeMemory(headers = "/home/ds/vlc/include/vlc/vlc.h")
//@NativeMemoryOptions(includes = {
//        "/home/ds/vlc/include",
//        "/usr/lib/llvm-15/lib/clang/15.0.7/include/"
//})
//@Structs
//@Enums
//@Unions
//public interface LibVLC {
//}
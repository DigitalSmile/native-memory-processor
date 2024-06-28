package io.github.digitalsmile;

import io.github.digitalsmile.annotation.NativeMemory;
import io.github.digitalsmile.annotation.NativeMemoryOptions;
import io.github.digitalsmile.annotation.function.Function;
import io.github.digitalsmile.annotation.function.NativeMemoryException;
import io.github.digitalsmile.annotation.function.Returns;
import io.github.digitalsmile.annotation.structure.Struct;
import io.github.digitalsmile.annotation.structure.Structs;

//@NativeMemory(header = "gpio.h",
//        options = @NativeMemoryOptions(
//                generateRootEnum = true
//        )
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
public interface Ioctl {

//    @Function(name = "ioctl", useErrno = true, returnType = int.class)
//    ChipInfo nativeCall(int fd, long command, @Returns ChipInfo data) throws NativeMemoryException;
}



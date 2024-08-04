package io.github.digitalsmile.gpio.types.custom;

import io.github.digitalsmile.annotation.NativeMemory;
import io.github.digitalsmile.annotation.NativeMemoryOptions;
import io.github.digitalsmile.annotation.structure.Enums;
import io.github.digitalsmile.annotation.structure.Struct;
import io.github.digitalsmile.annotation.structure.Structs;
import io.github.digitalsmile.annotation.structure.Unions;

@NativeMemory(headers = "/usr/src/linux-headers-${version}/include/uapi/linux/gpio.h")
@NativeMemoryOptions(
        processRootConstants = true
)
@Structs({
        @Struct(name = "gpiochip_info", javaName = "ChipInfo"),
        @Struct(name = "gpio_v2_line_attribute", javaName = "LineAttribute"),
        @Struct(name = "gpio_v2_line_config", javaName = "LineConfig"),
        @Struct(name = "gpio_v2_line_config_attribute", javaName = "LineConfigAttribute"),
        @Struct(name = "gpio_v2_line_event", javaName = "LineEvent"),
        @Struct(name = "gpio_v2_line_info", javaName = "LineInfo"),
        @Struct(name = "gpio_v2_line_request", javaName = "LineRequest"),
        @Struct(name = "gpio_v2_line_values", javaName = "LineValues")
})
@Enums
@Unions
public interface GPIOTest {
}

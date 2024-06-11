package org.digitalsmile;

@Native(header = "/usr/src/linux-headers-6.2.0-39/include/uapi/linux/gpio.h")
@Structs({
        @Struct(name = "gpiochip_info", javaName = "ChipInfo"),
        @Struct(name = "gpio_v2_line_info", javaName = "LineInfo"),
        @Struct(name = "gpio_v2_line_attribute", javaName = "LineAttribute"),
        @Struct(name = "gpio_v2_line_config", javaName = "LineConfig"),
        @Struct(name = "gpio_v2_line_request", javaName = "LineRequest"),
        @Struct(name = "gpio_v2_line_values", javaName = "LineValues"),
        @Struct(name = "gpio_v2_line_config_attribute", javaName = "LineConfigAttribute")
})
public interface GPIO {
}


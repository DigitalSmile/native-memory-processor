package io.github.digitalsmile.gpio.types.all;

import io.github.digitalsmile.annotation.NativeMemory;
import io.github.digitalsmile.annotation.NativeMemoryOptions;
import io.github.digitalsmile.annotation.structure.Enums;
import io.github.digitalsmile.annotation.structure.Structs;
import io.github.digitalsmile.annotation.structure.Unions;

@NativeMemory(headers = "/usr/src/linux-headers-${linux-version}/include/uapi/linux/gpio.h")
@NativeMemoryOptions(
        processRootConstants = true
)
@Structs
@Enums
@Unions
public interface GPIOTypesAll {
}
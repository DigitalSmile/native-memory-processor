package io.github.digitalsmile.gpio.libcurl;

import io.github.digitalsmile.annotation.NativeMemory;
import io.github.digitalsmile.annotation.NativeMemoryOptions;
import io.github.digitalsmile.annotation.structure.Enums;
import io.github.digitalsmile.annotation.structure.Structs;
import io.github.digitalsmile.annotation.structure.Unions;

@NativeMemory(headers = "/home/ds/curl/include/curl/curl.h")
@NativeMemoryOptions(systemIncludes = {
        "/usr/lib/llvm-15/lib/clang/15.0.7/include/"
})
@Structs
@Enums
@Unions
public interface Libcurl {
}

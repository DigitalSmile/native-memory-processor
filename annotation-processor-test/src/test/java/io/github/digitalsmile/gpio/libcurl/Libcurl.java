package io.github.digitalsmile.gpio.libcurl;

import io.github.digitalsmile.annotation.NativeMemory;
import io.github.digitalsmile.annotation.NativeMemoryException;
import io.github.digitalsmile.annotation.NativeMemoryOptions;
import io.github.digitalsmile.annotation.function.ByAddress;
import io.github.digitalsmile.annotation.function.NativeManualFunction;
import io.github.digitalsmile.annotation.structure.Enum;
import io.github.digitalsmile.annotation.structure.Enums;
import io.github.digitalsmile.annotation.structure.Struct;
import io.github.digitalsmile.annotation.structure.Structs;
import io.github.digitalsmile.gpio.libcurl.enums.CURLCode;
import io.github.digitalsmile.gpio.libcurl.enums.CURLOption;
import io.github.digitalsmile.gpio.libcurl.opaque.CurlInstance;

@NativeMemory(headers = "libcurl/curl/include/curl/curl.h")
@NativeMemoryOptions(systemIncludes = {
        "/usr/lib/gcc/x86_64-linux-gnu/${gcc-version}/include/"
}, debugMode = true, processRootConstants = true)
@Structs({
        @Struct(name = "CURL", javaName = "CurlInstance")
})
@Enums({
        @Enum(name = "CURLcode", javaName = "CURLCode"),
        @Enum(name = "CURLoption", javaName = "CURLOption")
})
public interface Libcurl {

    @NativeManualFunction(name = "curl_easy_init", library = "/usr/lib/x86_64-linux-gnu/libcurl.so")
    CurlInstance easyInit() throws NativeMemoryException;

    @NativeManualFunction(name = "curl_global_init", library = "/usr/lib/x86_64-linux-gnu/libcurl.so")
    CURLCode globalInit(long flags) throws NativeMemoryException;

    @NativeManualFunction(name = "curl_easy_setopt", library = "/usr/lib/x86_64-linux-gnu/libcurl.so")
    CURLCode easySetOpt(CurlInstance curl, CURLOption option, String value) throws NativeMemoryException;

    @NativeManualFunction(name = "curl_easy_setopt", library = "/usr/lib/x86_64-linux-gnu/libcurl.so")
    CURLCode easySetOpt(CurlInstance curl, CURLOption option, @ByAddress long value) throws NativeMemoryException;

    @NativeManualFunction(name = "curl_easy_perform", library = "/usr/lib/x86_64-linux-gnu/libcurl.so")
    CURLCode easyPerform(CurlInstance curl) throws NativeMemoryException;
}

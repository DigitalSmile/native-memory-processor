package io.github.digitalsmile.gpio.libcurl;

import io.github.digitalsmile.annotation.NativeMemory;
import io.github.digitalsmile.annotation.NativeMemoryException;
import io.github.digitalsmile.annotation.NativeMemoryOptions;
import io.github.digitalsmile.annotation.function.ByAddress;
import io.github.digitalsmile.annotation.function.NativeManualFunction;
import io.github.digitalsmile.annotation.library.NativeFunction;
import io.github.digitalsmile.annotation.library.NativeMemoryLibrary;
import io.github.digitalsmile.annotation.structure.Enum;
import io.github.digitalsmile.annotation.structure.Enums;
import io.github.digitalsmile.annotation.structure.Struct;
import io.github.digitalsmile.annotation.structure.Structs;
import io.github.digitalsmile.gpio.libcurl.enums.CURLcode;
import io.github.digitalsmile.gpio.libcurl.enums.CURLoption;
import io.github.digitalsmile.gpio.libcurl.opaque.CURL;

@NativeMemory(headers = "libcurl/curl/include/curl/curl.h")
@NativeMemoryOptions(systemIncludes = {
        //"/usr/lib/gcc/x86_64-linux-gnu/12/include/"
        "/usr/lib/llvm-15/lib/clang/15.0.7/include/"
}, debugMode = true, processRootConstants = true, systemHeader = true)
@Structs({
  //      @Struct(name = "CURL", javaName = "CurlInstance")
})
@Enums({
//        @Enum(name = "CURLcode", javaName = "CURLCode"),
//        @Enum(name = "CURLoption", javaName = "CURLOption")
})
@NativeMemoryLibrary
public interface Libcurl {

    @NativeManualFunction(name = "curl_easy_init", library = "/usr/lib/x86_64-linux-gnu/libcurl.so")
    CURL easyInit() throws NativeMemoryException;

    @NativeManualFunction(name = "curl_global_init", library = "/usr/lib/x86_64-linux-gnu/libcurl.so")
    CURLcode globalInit(long flags) throws NativeMemoryException;

    @NativeManualFunction(name = "curl_easy_setopt", library = "/usr/lib/x86_64-linux-gnu/libcurl.so")
    CURLcode easySetOpt(CURL curl, CURLoption option, String value) throws NativeMemoryException;

    @NativeManualFunction(name = "curl_easy_setopt", library = "/usr/lib/x86_64-linux-gnu/libcurl.so")
    CURLcode easySetOpt(CURL curl, CURLoption option, @ByAddress long value) throws NativeMemoryException;

    @NativeManualFunction(name = "curl_easy_perform", library = "/usr/lib/x86_64-linux-gnu/libcurl.so")
    CURLcode easyPerform(CURL curl) throws NativeMemoryException;
}

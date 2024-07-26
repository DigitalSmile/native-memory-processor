package io.github.digitalsmile.gpio.libvlc;

import io.github.digitalsmile.annotation.NativeMemory;
import io.github.digitalsmile.annotation.NativeMemoryOptions;
import io.github.digitalsmile.annotation.structure.Enums;
import io.github.digitalsmile.annotation.structure.Structs;
import io.github.digitalsmile.annotation.structure.Unions;

@NativeMemory(headers =  {
        "/home/ds/vlc/include/vlc/vlc.h",

})
@NativeMemoryOptions(includes = {
        "/home/ds/vlc/include"

}, systemIncludes =  "/usr/lib/llvm-15/lib/clang/15.0.7/include/", debugMode = true)
@Structs
@Enums
@Unions
public interface LibVLC {
}

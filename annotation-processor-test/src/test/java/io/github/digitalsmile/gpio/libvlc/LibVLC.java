package io.github.digitalsmile.gpio.libvlc;

import io.github.digitalsmile.annotation.NativeMemory;
import io.github.digitalsmile.annotation.NativeMemoryException;
import io.github.digitalsmile.annotation.NativeMemoryOptions;
import io.github.digitalsmile.annotation.function.NativeManualFunction;
import io.github.digitalsmile.annotation.structure.Enums;
import io.github.digitalsmile.annotation.structure.Structs;
import io.github.digitalsmile.annotation.structure.Unions;
import io.github.digitalsmile.annotation.types.StringArray;
import io.github.digitalsmile.gpio.libvlc.opaque.LibvlcInstanceT;
import io.github.digitalsmile.gpio.libvlc.opaque.LibvlcMediaPlayerT;
import io.github.digitalsmile.gpio.libvlc.opaque.LibvlcMediaT;
import io.github.digitalsmile.gpio.libvlc.structs.LibvlcModuleDescriptionT;

@NativeMemory(headers = "libvlc/vlc/include/vlc/vlc.h")
@NativeMemoryOptions(
        includes = "libvlc/vlc/include",
        systemIncludes = "/usr/lib/gcc/x86_64-linux-gnu/${gcc-version}/include/",
        debugMode = true
)
@Structs
@Enums
@Unions
public interface LibVLC {

    @NativeManualFunction(name = "libvlc_get_version", library = "/home/ds/vlc/lib/.libs/libvlc.so")
    String version() throws NativeMemoryException;

    @NativeManualFunction(name = "libvlc_new", library = "/home/ds/vlc/lib/.libs/libvlc.so")
    LibvlcInstanceT createInstance(int argc, StringArray segment) throws NativeMemoryException;

    @NativeManualFunction(name = "libvlc_audio_filter_list_get", library = "/home/ds/vlc/lib/.libs/libvlc.so")
    LibvlcModuleDescriptionT audioFilterModules(LibvlcInstanceT instance) throws NativeMemoryException;

    @NativeManualFunction(name = "libvlc_media_new_path", library = "/home/ds/vlc/lib/.libs/libvlc.so")
    LibvlcMediaT newPath(String path) throws NativeMemoryException;

    @NativeManualFunction(name = "libvlc_media_player_new_from_media", library = "/home/ds/vlc/lib/.libs/libvlc.so")
    LibvlcMediaPlayerT mediaPlayer(LibvlcInstanceT instance, LibvlcMediaT media) throws NativeMemoryException;

    @NativeManualFunction(name = "libvlc_media_player_play", library = "/home/ds/vlc/lib/.libs/libvlc.so")
    int play(LibvlcMediaPlayerT player) throws NativeMemoryException;

    @NativeManualFunction(name = "libvlc_media_player_is_playing", library = "/home/ds/vlc/lib/.libs/libvlc.so")
    boolean isPlaying(LibvlcMediaPlayerT player) throws NativeMemoryException;

    @NativeManualFunction(name = "libvlc_media_player_pause", library = "/home/ds/vlc/lib/.libs/libvlc.so")
    void pause(LibvlcMediaPlayerT player) throws NativeMemoryException;

    @NativeManualFunction(name = "libvlc_media_player_get_time", library = "/home/ds/vlc/lib/.libs/libvlc.so")
    long time(LibvlcMediaPlayerT player) throws NativeMemoryException;

    @NativeManualFunction(name = "libvlc_media_player_get_position", library = "/home/ds/vlc/lib/.libs/libvlc.so")
    double position(LibvlcMediaPlayerT player) throws NativeMemoryException;

    @NativeManualFunction(name = "libvlc_errmsg", library = "/home/ds/vlc/lib/.libs/libvlc.so")
    String errorMessage() throws NativeMemoryException;
}

package io.github.digitalsmile.annotation.types.interfaces;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

public interface OpaqueMemoryLayout extends NativeMemoryLayout {
    MemorySegment memorySegment();

    default MemoryLayout getMemoryLayout() {
        return ValueLayout.ADDRESS;
    }
}

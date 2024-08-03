package io.github.digitalsmile.annotation.types.interfaces;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;

public interface NativeMemoryContext extends SegmentAllocator {
    Arena getArena();
    void checkIsCreatedByArena(MemorySegment segment);
}

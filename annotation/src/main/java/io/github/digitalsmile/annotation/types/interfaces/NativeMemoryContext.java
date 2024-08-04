package io.github.digitalsmile.annotation.types.interfaces;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;

/**
 * Interface for holding the context for accessing native functions.
 * <p>
 * Usually holds the <code>Arena</code>, native library reference and native function descriptors.
 */
public interface NativeMemoryContext extends SegmentAllocator, AutoCloseable {
    /**
     * Gets the backed <code>Arena</code> object.
     *
     * @return backed arena instance
     */
    Arena getArena();

    /**
     * Checks if memory segment was created by backed <code>Arena</code> or registered scope.
     * See {@link io.github.digitalsmile.annotation.function.NativeCall}.
     *
     * @param segment memory segment to be checked
     */
    void checkIsCreatedByArena(MemorySegment segment);
}

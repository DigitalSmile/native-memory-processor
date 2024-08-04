package io.github.digitalsmile.annotation.types.interfaces;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 * Special case for {@link NativeMemoryLayout} instance, which does not have a public structure description (opaque structure).
 * Holds only the memory segment (pointer) to the structure, but the content is always unknown.
 */
public interface OpaqueMemoryLayout extends NativeMemoryLayout {
    /**
     * Gets memory segment (pointer) backed by opaque structure.
     *
     * @return backed memory segment (pointer)
     */
    MemorySegment memorySegment();

    /**
     * Gets Memory layout of opaque structure, which should be always <code>ValueLayout.ADDRESS</code>.
     *
     * @return address value layout
     */
    default MemoryLayout getMemoryLayout() {
        return ValueLayout.ADDRESS;
    }
}

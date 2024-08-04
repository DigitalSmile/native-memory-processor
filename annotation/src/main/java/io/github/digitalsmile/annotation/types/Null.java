package io.github.digitalsmile.annotation.types;

import io.github.digitalsmile.annotation.types.interfaces.OpaqueMemoryLayout;

import java.lang.foreign.MemorySegment;

/**
 * Static helper for NULL structure, which is always a pointer of type (void *0)
 */
public record Null() implements OpaqueMemoryLayout {

    @Override
    public MemorySegment memorySegment() {
        return MemorySegment.NULL;
    }


    @SuppressWarnings("unchecked")
    @Override
    public Null fromBytes(MemorySegment buffer) throws Throwable {
        return new Null();
    }

    @Override
    public void toBytes(MemorySegment buffer) throws Throwable {
        // do nothing since this is NULL
    }

    @Override
    public boolean isEmpty() {
        return true;
    }
}

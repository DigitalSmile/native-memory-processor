package io.github.digitalsmile.annotation.types;

import io.github.digitalsmile.annotation.types.interfaces.OpaqueMemoryLayout;

import java.lang.foreign.MemorySegment;

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
        // do nothing
    }

    @Override
    public boolean isEmpty() {
        return true;
    }
}

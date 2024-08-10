package io.github.digitalsmile.annotation.types;

import io.github.digitalsmile.annotation.types.interfaces.OpaqueMemoryLayout;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

public record RawPointer(MemorySegment memorySegment) implements OpaqueMemoryLayout {
    public static final MemoryLayout LAYOUT = ValueLayout.ADDRESS;

    public static RawPointer create(MemorySegment memorySegment) {
        return new RawPointer(memorySegment);
    }

    public static RawPointer createEmpty() {
        return new RawPointer(MemorySegment.NULL);
    }

    @Override
    public MemoryLayout getMemoryLayout() {
        return LAYOUT;
    }

    @Override
    @SuppressWarnings("unchecked")
    public RawPointer fromBytes(MemorySegment buffer) throws Throwable {
        return new RawPointer(buffer);
    }

    @Override
    public void toBytes(MemorySegment buffer) throws Throwable {
        memorySegment().copyFrom(buffer);
    }

    @Override
    public boolean isEmpty() {
        return memorySegment.equals(MemorySegment.NULL);
    }
}

package io.github.digitalsmile.annotation.types;

import io.github.digitalsmile.annotation.types.interfaces.NativeMemoryLayout;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public record StringArray(String... strings) implements NativeMemoryLayout {
    public static final MemoryLayout LAYOUT = MemoryLayout.sequenceLayout(1024, ValueLayout.ADDRESS);

    public static StringArray createEmpty() {
        return new StringArray();
    }

    @Override
    public MemoryLayout getMemoryLayout() {
        return LAYOUT;
    }

    @SuppressWarnings("unchecked")
    @Override
    public StringArray fromBytes(MemorySegment buffer) throws Throwable {
        List<String> stringList = new ArrayList<>();
        for (int i = 0; i < buffer.byteSize() / 8; i++) {
            var tmp = buffer.getAtIndex(ValueLayout.ADDRESS, i);
            if (tmp.equals(MemorySegment.NULL)) {
                continue;
            }
            stringList.add(tmp.reinterpret(Integer.MAX_VALUE).getString(0));
        }
        return new StringArray(stringList.toArray(String[]::new));
    }

    @Override
    public void toBytes(MemorySegment buffer) throws Throwable {
        for (int i = 0; i < strings.length; i++) {
            var buff = Arena.global().allocateFrom(strings[i]);
            buffer.setAtIndex(ValueLayout.ADDRESS, i, buff);
        }
    }

    @Override
    public boolean isEmpty() {
        return strings.length == 0;
    }

    @Override
    public String toString() {
        return Arrays.toString(strings);
    }
}

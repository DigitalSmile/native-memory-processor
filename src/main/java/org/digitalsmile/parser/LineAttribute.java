package org.digitalsmile.parser;

import org.digitalsmile.NativeMemoryLayout;
import org.openjdk.jextract.clang.Index;

import java.lang.constant.Constable;
import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.util.Arrays;

public record LineAttribute(int id, int padding, long flags, long values, int debounce_period_us) implements NativeMemoryLayout {
    public static final MemoryLayout LAYOUT = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT.withName("id"),
            ValueLayout.JAVA_INT.withName("padding"),
            MemoryLayout.unionLayout(
                    ValueLayout.JAVA_LONG.withName("flags"),
                    ValueLayout.JAVA_LONG.withName("values"),
                    ValueLayout.JAVA_INT.withName("debounce_period_us")
            ).withName("union_1")
    );

    private static final VarHandle VH_ID = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("id"));

    private static final VarHandle VH_PADDING = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("padding"));

    private static final VarHandle VH_FLAGS = LAYOUT.select(MemoryLayout.PathElement.groupElement("union_1")).varHandle(MemoryLayout.PathElement.groupElement("flags"));

    private static final VarHandle VH_VALUES = LAYOUT.select(MemoryLayout.PathElement.groupElement("union_1")).varHandle(MemoryLayout.PathElement.groupElement("values"));

    private static final VarHandle VH_DEBOUNCE_PERIOD_US = LAYOUT.select(MemoryLayout.PathElement.groupElement("union_1")).varHandle(MemoryLayout.PathElement.groupElement("debounce_period_us"));

    public static LineAttribute createEmpty() {
        return new LineAttribute(0, 0, 0, 0, 0);
    }
    public static LineAttribute createWithFlags(int id, int padding, long flags) {
        return new LineAttribute(id, padding, flags, 0, 0);
    }
    public static LineAttribute createWithValues(int id, int padding, long values) {
        return new LineAttribute(id, padding, 0, values, 0);
    }
    public static LineAttribute createWithDebouncePeriodUs(int id, int padding, int debounce_period_us) {
        return new LineAttribute(id, padding, 0, 0, debounce_period_us);
    }


    @Override
    @SuppressWarnings("unchecked")
    public LineAttribute fromBytes(MemorySegment buffer) throws Throwable {
        var unionSize = LAYOUT.select(MemoryLayout.PathElement.groupElement("union_1")).byteSize();
        var unionBuffer = buffer.asSlice(LAYOUT.byteSize() - unionSize, unionSize);
        return new LineAttribute(
                (int) VH_ID.get(buffer, 0L),
                (int) VH_PADDING.get(buffer, 0L),
                (long) VH_FLAGS.get(unionBuffer, 0L),
                (long) VH_VALUES.get(unionBuffer, 0L),
                (int) VH_DEBOUNCE_PERIOD_US.get(unionBuffer, 0L));
    }

    @Override
    public void toBytes(MemorySegment buffer) throws Throwable {
        VH_ID.set(buffer, 0L, id);
        VH_PADDING.set(buffer, 0L, padding);
        var unionSize = LAYOUT.select(MemoryLayout.PathElement.groupElement("union_1")).byteSize();
        var unionBuffer = buffer.asSlice(LAYOUT.byteSize() - unionSize, unionSize);
        if (flags != 0) {
            VH_FLAGS.set(unionBuffer, 0L, flags);
        } else if (values != 0) {
            VH_VALUES.set(unionBuffer, 0L, values);
        } else if (debounce_period_us != 0) {
            VH_DEBOUNCE_PERIOD_US.set(unionBuffer, 0L, debounce_period_us);
        }
    }

    @Override
    public boolean isEmpty() {
        return false;
    }


    public static void main(String[] args) throws Throwable {
        var offheap = Arena.ofConfined();
        var a = LineAttribute.createWithValues(1, 2, 3);
        var buffer = offheap.allocate(LineAttribute.LAYOUT.byteSize(), LineAttribute.LAYOUT.byteAlignment());
        a.toBytes(buffer);
        var b = LineAttribute.createEmpty().fromBytes(buffer);

        System.out.println(a);
        System.out.println(b);
    }
}
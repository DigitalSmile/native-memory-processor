/*
 *  Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 *
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *   Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 */

// Generated by jextract

package org.openjdk.jextract.clang.libclang;

import java.lang.invoke.*;
import java.lang.foreign.*;
import java.util.function.*;

import static java.lang.foreign.ValueLayout.*;
import static java.lang.foreign.MemoryLayout.PathElement.*;

/**
 * {@snippet lang=c :
 * struct {
 *     enum CXCursorKind kind;
 *     int xdata;
 *     const void *data[3];
 * }
 * }
 */
public class CXCursor {

    CXCursor() {
        // Should not be called directly
    }

    private static final GroupLayout $LAYOUT = MemoryLayout.structLayout(
        Index_h.C_INT.withName("kind"),
        Index_h.C_INT.withName("xdata"),
        MemoryLayout.sequenceLayout(3, Index_h.C_POINTER).withName("data")
    ).withName("$anon$2706:9");

    /**
     * The layout of this struct
     */
    public static final GroupLayout layout() {
        return $LAYOUT;
    }

    private static final OfInt kind$LAYOUT = (OfInt)$LAYOUT.select(groupElement("kind"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * enum CXCursorKind kind
     * }
     */
    public static final OfInt kind$layout() {
        return kind$LAYOUT;
    }

    private static final long kind$OFFSET = 0;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * enum CXCursorKind kind
     * }
     */
    public static final long kind$offset() {
        return kind$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * enum CXCursorKind kind
     * }
     */
    public static int kind(MemorySegment struct) {
        return struct.get(kind$LAYOUT, kind$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * enum CXCursorKind kind
     * }
     */
    public static void kind(MemorySegment struct, int fieldValue) {
        struct.set(kind$LAYOUT, kind$OFFSET, fieldValue);
    }

    private static final OfInt xdata$LAYOUT = (OfInt)$LAYOUT.select(groupElement("xdata"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int xdata
     * }
     */
    public static final OfInt xdata$layout() {
        return xdata$LAYOUT;
    }

    private static final long xdata$OFFSET = 4;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int xdata
     * }
     */
    public static final long xdata$offset() {
        return xdata$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int xdata
     * }
     */
    public static int xdata(MemorySegment struct) {
        return struct.get(xdata$LAYOUT, xdata$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int xdata
     * }
     */
    public static void xdata(MemorySegment struct, int fieldValue) {
        struct.set(xdata$LAYOUT, xdata$OFFSET, fieldValue);
    }

    private static final SequenceLayout data$LAYOUT = (SequenceLayout)$LAYOUT.select(groupElement("data"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * const void *data[3]
     * }
     */
    public static final SequenceLayout data$layout() {
        return data$LAYOUT;
    }

    private static final long data$OFFSET = 8;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * const void *data[3]
     * }
     */
    public static final long data$offset() {
        return data$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * const void *data[3]
     * }
     */
    public static MemorySegment data(MemorySegment struct) {
        return struct.asSlice(data$OFFSET, data$LAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * const void *data[3]
     * }
     */
    public static void data(MemorySegment struct, MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, data$OFFSET, data$LAYOUT.byteSize());
    }

    private static long[] data$DIMS = { 3 };

    /**
     * Dimensions for array field:
     * {@snippet lang=c :
     * const void *data[3]
     * }
     */
    public static long[] data$dimensions() {
        return data$DIMS;
    }
    private static final VarHandle data$ELEM_HANDLE = data$LAYOUT.varHandle(sequenceElement());

    /**
     * Indexed getter for field:
     * {@snippet lang=c :
     * const void *data[3]
     * }
     */
    public static MemorySegment data(MemorySegment struct, long index0) {
        return (MemorySegment)data$ELEM_HANDLE.get(struct, 0L, index0);
    }

    /**
     * Indexed setter for field:
     * {@snippet lang=c :
     * const void *data[3]
     * }
     */
    public static void data(MemorySegment struct, long index0, MemorySegment fieldValue) {
        data$ELEM_HANDLE.set(struct, 0L, index0, fieldValue);
    }

    /**
     * Obtains a slice of {@code arrayParam} which selects the array element at {@code index}.
     * The returned segment has address {@code arrayParam.address() + index * layout().byteSize()}
     */
    public static MemorySegment asSlice(MemorySegment array, long index) {
        return array.asSlice(layout().byteSize() * index);
    }

    /**
     * The size (in bytes) of this struct
     */
    public static long sizeof() { return layout().byteSize(); }

    /**
     * Allocate a segment of size {@code layout().byteSize()} using {@code allocator}
     */
    public static MemorySegment allocate(SegmentAllocator allocator) {
        return allocator.allocate(layout());
    }

    /**
     * Allocate an array of size {@code elementCount} using {@code allocator}.
     * The returned segment has size {@code elementCount * layout().byteSize()}.
     */
    public static MemorySegment allocateArray(long elementCount, SegmentAllocator allocator) {
        return allocator.allocate(MemoryLayout.sequenceLayout(elementCount, layout()));
    }

    /**
     * Reinterprets {@code addr} using target {@code arena} and {@code cleanupAction) (if any).
     * The returned segment has size {@code layout().byteSize()}
     */
    public static MemorySegment reinterpret(MemorySegment addr, Arena arena, Consumer<MemorySegment> cleanup) {
        return reinterpret(addr, 1, arena, cleanup);
    }

    /**
     * Reinterprets {@code addr} using target {@code arena} and {@code cleanupAction) (if any).
     * The returned segment has size {@code elementCount * layout().byteSize()}
     */
    public static MemorySegment reinterpret(MemorySegment addr, long elementCount, Arena arena, Consumer<MemorySegment> cleanup) {
        return addr.reinterpret(layout().byteSize() * elementCount, arena, cleanup);
    }
}


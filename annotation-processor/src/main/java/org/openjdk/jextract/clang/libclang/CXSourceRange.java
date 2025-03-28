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
 *     const void *ptr_data[2];
 *     unsigned int begin_int_data;
 *     unsigned int end_int_data;
 * }
 * }
 */
public class CXSourceRange {

    CXSourceRange() {
        // Should not be called directly
    }

    private static final GroupLayout $LAYOUT = MemoryLayout.structLayout(
        MemoryLayout.sequenceLayout(2, Index_h.C_POINTER).withName("ptr_data"),
        Index_h.C_INT.withName("begin_int_data"),
        Index_h.C_INT.withName("end_int_data")
    ).withName("$anon$467:9");

    /**
     * The layout of this struct
     */
    public static final GroupLayout layout() {
        return $LAYOUT;
    }

    private static final SequenceLayout ptr_data$LAYOUT = (SequenceLayout)$LAYOUT.select(groupElement("ptr_data"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * const void *ptr_data[2]
     * }
     */
    public static final SequenceLayout ptr_data$layout() {
        return ptr_data$LAYOUT;
    }

    private static final long ptr_data$OFFSET = 0;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * const void *ptr_data[2]
     * }
     */
    public static final long ptr_data$offset() {
        return ptr_data$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * const void *ptr_data[2]
     * }
     */
    public static MemorySegment ptr_data(MemorySegment struct) {
        return struct.asSlice(ptr_data$OFFSET, ptr_data$LAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * const void *ptr_data[2]
     * }
     */
    public static void ptr_data(MemorySegment struct, MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, ptr_data$OFFSET, ptr_data$LAYOUT.byteSize());
    }

    private static long[] ptr_data$DIMS = { 2 };

    /**
     * Dimensions for array field:
     * {@snippet lang=c :
     * const void *ptr_data[2]
     * }
     */
    public static long[] ptr_data$dimensions() {
        return ptr_data$DIMS;
    }
    private static final VarHandle ptr_data$ELEM_HANDLE = ptr_data$LAYOUT.varHandle(sequenceElement());

    /**
     * Indexed getter for field:
     * {@snippet lang=c :
     * const void *ptr_data[2]
     * }
     */
    public static MemorySegment ptr_data(MemorySegment struct, long index0) {
        return (MemorySegment)ptr_data$ELEM_HANDLE.get(struct, 0L, index0);
    }

    /**
     * Indexed setter for field:
     * {@snippet lang=c :
     * const void *ptr_data[2]
     * }
     */
    public static void ptr_data(MemorySegment struct, long index0, MemorySegment fieldValue) {
        ptr_data$ELEM_HANDLE.set(struct, 0L, index0, fieldValue);
    }

    private static final OfInt begin_int_data$LAYOUT = (OfInt)$LAYOUT.select(groupElement("begin_int_data"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * unsigned int begin_int_data
     * }
     */
    public static final OfInt begin_int_data$layout() {
        return begin_int_data$LAYOUT;
    }

    private static final long begin_int_data$OFFSET = 16;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * unsigned int begin_int_data
     * }
     */
    public static final long begin_int_data$offset() {
        return begin_int_data$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned int begin_int_data
     * }
     */
    public static int begin_int_data(MemorySegment struct) {
        return struct.get(begin_int_data$LAYOUT, begin_int_data$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned int begin_int_data
     * }
     */
    public static void begin_int_data(MemorySegment struct, int fieldValue) {
        struct.set(begin_int_data$LAYOUT, begin_int_data$OFFSET, fieldValue);
    }

    private static final OfInt end_int_data$LAYOUT = (OfInt)$LAYOUT.select(groupElement("end_int_data"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * unsigned int end_int_data
     * }
     */
    public static final OfInt end_int_data$layout() {
        return end_int_data$LAYOUT;
    }

    private static final long end_int_data$OFFSET = 20;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * unsigned int end_int_data
     * }
     */
    public static final long end_int_data$offset() {
        return end_int_data$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned int end_int_data
     * }
     */
    public static int end_int_data(MemorySegment struct) {
        return struct.get(end_int_data$LAYOUT, end_int_data$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned int end_int_data
     * }
     */
    public static void end_int_data(MemorySegment struct, int fieldValue) {
        struct.set(end_int_data$LAYOUT, end_int_data$OFFSET, fieldValue);
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


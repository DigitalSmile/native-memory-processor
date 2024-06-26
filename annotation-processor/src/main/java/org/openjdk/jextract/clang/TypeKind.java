/*
 *  Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 *
 */
package org.openjdk.jextract.clang;

import org.openjdk.jextract.clang.libclang.Index_h;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public enum TypeKind {

    Invalid(Index_h.CXType_Invalid()),
    Unexposed(Index_h.CXType_Unexposed()),
    Void(Index_h.CXType_Void()),
    Bool(Index_h.CXType_Bool()),
    Char_U(Index_h.CXType_Char_U()),
    UChar(Index_h.CXType_UChar()),
    Char16(Index_h.CXType_Char16()),
    Char32(Index_h.CXType_Char32()),
    UShort(Index_h.CXType_UShort()),
    UInt(Index_h.CXType_UInt()),
    ULong(Index_h.CXType_ULong()),
    ULongLong(Index_h.CXType_ULongLong()),
    UInt128(Index_h.CXType_UInt128()),
    Char_S(Index_h.CXType_Char_S()),
    SChar(Index_h.CXType_SChar()),
    WChar(Index_h.CXType_WChar()),
    Short(Index_h.CXType_Short()),
    Int(Index_h.CXType_Int()),
    Long(Index_h.CXType_Long()),
    LongLong(Index_h.CXType_LongLong()),
    Int128(Index_h.CXType_Int128()),
    Float(Index_h.CXType_Float()),
    Double(Index_h.CXType_Double()),
    LongDouble(Index_h.CXType_LongDouble()),
    NullPtr(Index_h.CXType_NullPtr()),
    Overload(Index_h.CXType_Overload()),
    Dependent(Index_h.CXType_Dependent()),
    ObjCId(Index_h.CXType_ObjCId()),
    ObjCClass(Index_h.CXType_ObjCClass()),
    ObjCSel(Index_h.CXType_ObjCSel()),
    Float128(Index_h.CXType_Float128()),
    Half(Index_h.CXType_Half()),
    Float16(Index_h.CXType_Float16()),
    ShortAccum(Index_h.CXType_ShortAccum()),
    Accum(Index_h.CXType_Accum()),
    LongAccum(Index_h.CXType_LongAccum()),
    UShortAccum(Index_h.CXType_UShortAccum()),
    UAccum(Index_h.CXType_UAccum()),
    ULongAccum(Index_h.CXType_ULongAccum()),
    Complex(Index_h.CXType_Complex()),
    Pointer(Index_h.CXType_Pointer()),
    BlockPointer(Index_h.CXType_BlockPointer()),
    LValueReference(Index_h.CXType_LValueReference()),
    RValueReference(Index_h.CXType_RValueReference()),
    Record(Index_h.CXType_Record()),
    Enum(Index_h.CXType_Enum()),
    Typedef(Index_h.CXType_Typedef()),
    ObjCInterface(Index_h.CXType_ObjCInterface()),
    ObjCObjectPointer(Index_h.CXType_ObjCObjectPointer()),
    FunctionNoProto(Index_h.CXType_FunctionNoProto()),
    FunctionProto(Index_h.CXType_FunctionProto()),
    ConstantArray(Index_h.CXType_ConstantArray()),
    Vector(Index_h.CXType_Vector()),
    IncompleteArray(Index_h.CXType_IncompleteArray()),
    VariableArray(Index_h.CXType_VariableArray()),
    DependentSizedArray(Index_h.CXType_DependentSizedArray()),
    MemberPointer(Index_h.CXType_MemberPointer()),
    Auto(Index_h.CXType_Auto()),
    Elaborated(Index_h.CXType_Elaborated()),
    Pipe(Index_h.CXType_Pipe()),
    OCLImage1dRO(Index_h.CXType_OCLImage1dRO()),
    OCLImage1dArrayRO(Index_h.CXType_OCLImage1dArrayRO()),
    OCLImage1dBufferRO(Index_h.CXType_OCLImage1dBufferRO()),
    OCLImage2dRO(Index_h.CXType_OCLImage2dRO()),
    OCLImage2dArrayRO(Index_h.CXType_OCLImage2dArrayRO()),
    OCLImage2dDepthRO(Index_h.CXType_OCLImage2dDepthRO()),
    OCLImage2dArrayDepthRO(Index_h.CXType_OCLImage2dArrayDepthRO()),
    OCLImage2dMSAARO(Index_h.CXType_OCLImage2dMSAARO()),
    OCLImage2dArrayMSAARO(Index_h.CXType_OCLImage2dArrayMSAARO()),
    OCLImage2dMSAADepthRO(Index_h.CXType_OCLImage2dMSAADepthRO()),
    OCLImage2dArrayMSAADepthRO(Index_h.CXType_OCLImage2dArrayMSAADepthRO()),
    OCLImage3dRO(Index_h.CXType_OCLImage3dRO()),
    OCLImage1dWO(Index_h.CXType_OCLImage1dWO()),
    OCLImage1dArrayWO(Index_h.CXType_OCLImage1dArrayWO()),
    OCLImage1dBufferWO(Index_h.CXType_OCLImage1dBufferWO()),
    OCLImage2dWO(Index_h.CXType_OCLImage2dWO()),
    OCLImage2dArrayWO(Index_h.CXType_OCLImage2dArrayWO()),
    OCLImage2dDepthWO(Index_h.CXType_OCLImage2dDepthWO()),
    OCLImage2dArrayDepthWO(Index_h.CXType_OCLImage2dArrayDepthWO()),
    OCLImage2dMSAAWO(Index_h.CXType_OCLImage2dMSAAWO()),
    OCLImage2dArrayMSAAWO(Index_h.CXType_OCLImage2dArrayMSAAWO()),
    OCLImage2dMSAADepthWO(Index_h.CXType_OCLImage2dMSAADepthWO()),
    OCLImage2dArrayMSAADepthWO(Index_h.CXType_OCLImage2dArrayMSAADepthWO()),
    OCLImage3dWO(Index_h.CXType_OCLImage3dWO()),
    OCLImage1dRW(Index_h.CXType_OCLImage1dRW()),
    OCLImage1dArrayRW(Index_h.CXType_OCLImage1dArrayRW()),
    OCLImage1dBufferRW(Index_h.CXType_OCLImage1dBufferRW()),
    OCLImage2dRW(Index_h.CXType_OCLImage2dRW()),
    OCLImage2dArrayRW(Index_h.CXType_OCLImage2dArrayRW()),
    OCLImage2dDepthRW(Index_h.CXType_OCLImage2dDepthRW()),
    OCLImage2dArrayDepthRW(Index_h.CXType_OCLImage2dArrayDepthRW()),
    OCLImage2dMSAARW(Index_h.CXType_OCLImage2dMSAARW()),
    OCLImage2dArrayMSAARW(Index_h.CXType_OCLImage2dArrayMSAARW()),
    OCLImage2dMSAADepthRW(Index_h.CXType_OCLImage2dMSAADepthRW()),
    OCLImage2dArrayMSAADepthRW(Index_h.CXType_OCLImage2dArrayMSAADepthRW()),
    OCLImage3dRW(Index_h.CXType_OCLImage3dRW()),
    OCLSampler(Index_h.CXType_OCLSampler()),
    OCLEvent(Index_h.CXType_OCLEvent()),
    OCLQueue(Index_h.CXType_OCLQueue()),
    OCLReserveID(Index_h.CXType_OCLReserveID()),
    ObjCObject(Index_h.CXType_ObjCObject()),
    ObjCTypeParam(Index_h.CXType_ObjCTypeParam()),
    Attributed(Index_h.CXType_Attributed()),
    OCLIntelSubgroupAVCMcePayload(Index_h.CXType_OCLIntelSubgroupAVCMcePayload()),
    OCLIntelSubgroupAVCImePayload(Index_h.CXType_OCLIntelSubgroupAVCImePayload()),
    OCLIntelSubgroupAVCRefPayload(Index_h.CXType_OCLIntelSubgroupAVCRefPayload()),
    OCLIntelSubgroupAVCSicPayload(Index_h.CXType_OCLIntelSubgroupAVCSicPayload()),
    OCLIntelSubgroupAVCMceResult(Index_h.CXType_OCLIntelSubgroupAVCMceResult()),
    OCLIntelSubgroupAVCImeResult(Index_h.CXType_OCLIntelSubgroupAVCImeResult()),
    OCLIntelSubgroupAVCRefResult(Index_h.CXType_OCLIntelSubgroupAVCRefResult()),
    OCLIntelSubgroupAVCSicResult(Index_h.CXType_OCLIntelSubgroupAVCSicResult()),
    OCLIntelSubgroupAVCImeResultSingleRefStreamout(Index_h.CXType_OCLIntelSubgroupAVCImeResultSingleRefStreamout()),
    OCLIntelSubgroupAVCImeResultDualRefStreamout(Index_h.CXType_OCLIntelSubgroupAVCImeResultDualRefStreamout()),
    OCLIntelSubgroupAVCImeSingleRefStreamin(Index_h.CXType_OCLIntelSubgroupAVCImeSingleRefStreamin()),
    OCLIntelSubgroupAVCImeDualRefStreamin(Index_h.CXType_OCLIntelSubgroupAVCImeDualRefStreamin()),
    ExtVector(Index_h.CXType_ExtVector()),
    Atomic(177);  // This is missing in auto-generated code

    private final int value;

    TypeKind(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    private final static Map<Integer, TypeKind> lookup;

    static {
        lookup = new HashMap<>();
        for (TypeKind e: TypeKind.values()) {
            lookup.put(e.value(), e);
        }
    }

    public final static TypeKind valueOf(int value) {
        TypeKind x = lookup.get(value);
        if (null == x) {
            throw new NoSuchElementException("kind = " + value);
        }
        return x;
    }
}

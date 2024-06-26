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

public enum CursorKind {

    UnexposedDecl(Index_h.CXCursor_UnexposedDecl()),
    StructDecl(Index_h.CXCursor_StructDecl()),
    UnionDecl(Index_h.CXCursor_UnionDecl()),
    ClassDecl(Index_h.CXCursor_ClassDecl()),
    EnumDecl(Index_h.CXCursor_EnumDecl()),
    FieldDecl(Index_h.CXCursor_FieldDecl()),
    EnumConstantDecl(Index_h.CXCursor_EnumConstantDecl()),
    FunctionDecl(Index_h.CXCursor_FunctionDecl()),
    VarDecl(Index_h.CXCursor_VarDecl()),
    ParmDecl(Index_h.CXCursor_ParmDecl()),
    TypedefDecl(Index_h.CXCursor_TypedefDecl()),
    Namespace(Index_h.CXCursor_Namespace()),
    TypeRef(Index_h.CXCursor_TypeRef()),
    IntegerLiteral(Index_h.CXCursor_IntegerLiteral()),
    FloatingLiteral(Index_h.CXCursor_FloatingLiteral()),
    ImaginaryLiteral(Index_h.CXCursor_ImaginaryLiteral()),
    StringLiteral(Index_h.CXCursor_StringLiteral()),
    CharacterLiteral(Index_h.CXCursor_CharacterLiteral()),
    UnexposedAttr(Index_h.CXCursor_UnexposedAttr()),
    IBActionAttr(Index_h.CXCursor_IBActionAttr()),
    IBOutletAttr(Index_h.CXCursor_IBOutletAttr()),
    IBOutletCollectionAttr(Index_h.CXCursor_IBOutletCollectionAttr()),
    CXXFinalAttr(Index_h.CXCursor_CXXFinalAttr()),
    CXXOverrideAttr(Index_h.CXCursor_CXXOverrideAttr()),
    AnnotateAttr(Index_h.CXCursor_AnnotateAttr()),
    AsmLabelAttr(Index_h.CXCursor_AsmLabelAttr()),
    PackedAttr(Index_h.CXCursor_PackedAttr()),
    PureAttr(Index_h.CXCursor_PureAttr()),
    ConstAttr(Index_h.CXCursor_ConstAttr()),
    NoDuplicateAttr(Index_h.CXCursor_NoDuplicateAttr()),
    CUDAConstantAttr(Index_h.CXCursor_CUDAConstantAttr()),
    CUDADeviceAttr(Index_h.CXCursor_CUDADeviceAttr()),
    CUDAGlobalAttr(Index_h.CXCursor_CUDAGlobalAttr()),
    CUDAHostAttr(Index_h.CXCursor_CUDAHostAttr()),
    CUDASharedAttr(Index_h.CXCursor_CUDASharedAttr()),
    VisibilityAttr(Index_h.CXCursor_VisibilityAttr()),
    DLLExport(Index_h.CXCursor_DLLExport()),
    DLLImport(Index_h.CXCursor_DLLImport()),
    NSReturnsRetained(Index_h.CXCursor_NSReturnsRetained()),
    NSReturnsNotRetained(Index_h.CXCursor_NSReturnsNotRetained()),
    NSReturnsAutoreleased(Index_h.CXCursor_NSReturnsAutoreleased()),
    NSConsumesSelf(Index_h.CXCursor_NSConsumesSelf()),
    NSConsumed(Index_h.CXCursor_NSConsumed()),
    ObjCException(Index_h.CXCursor_ObjCException()),
    ObjCNSObject(Index_h.CXCursor_ObjCNSObject()),
    ObjCIndependentClass(Index_h.CXCursor_ObjCIndependentClass()),
    ObjCPreciseLifetime(Index_h.CXCursor_ObjCPreciseLifetime()),
    ObjCReturnsInnerPointer(Index_h.CXCursor_ObjCReturnsInnerPointer()),
    ObjCRequiresSuper(Index_h.CXCursor_ObjCRequiresSuper()),
    ObjCRootClass(Index_h.CXCursor_ObjCRootClass()),
    ObjCSubclassingRestricted(Index_h.CXCursor_ObjCSubclassingRestricted()),
    ObjCExplicitProtocolImpl(Index_h.CXCursor_ObjCExplicitProtocolImpl()),
    ObjCDesignatedInitializer(Index_h.CXCursor_ObjCDesignatedInitializer()),
    ObjCRuntimeVisible(Index_h.CXCursor_ObjCRuntimeVisible()),
    ObjCBoxable(Index_h.CXCursor_ObjCBoxable()),
    FlagEnum(Index_h.CXCursor_FlagEnum()),
    ConvergentAttr(Index_h.CXCursor_ConvergentAttr()),
    WarnUnusedAttr(Index_h.CXCursor_WarnUnusedAttr()),
    WarnUnusedResultAttr(Index_h.CXCursor_WarnUnusedResultAttr()),
    AlignedAttr(Index_h.CXCursor_AlignedAttr()),
    MacroDefinition(Index_h.CXCursor_MacroDefinition()),
    MacroExpansion(Index_h.CXCursor_MacroExpansion()),
    MacroInstantiation(Index_h.CXCursor_MacroInstantiation()),
    InclusionDirective(Index_h.CXCursor_InclusionDirective()),
    /*
     * Per libclang API docs, clang returns this CursorKind
     * for both C11 _Static_assert and C++11 static_assert
     */
    StaticAssert(Index_h.CXCursor_StaticAssert());


    private final int value;

    CursorKind(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    private final static Map<Integer, CursorKind> lookup;

    static {
        lookup = new HashMap<>();
        for (CursorKind e: CursorKind.values()) {
            lookup.put(e.value(), e);
        }
    }

    public final static CursorKind valueOf(int value) {
        CursorKind x = lookup.get(value);
        if (null == x) {
            throw new NoSuchElementException("Invalid Cursor kind value: " + value);
        }
        return x;
    }
}

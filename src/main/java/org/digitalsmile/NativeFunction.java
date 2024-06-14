package org.digitalsmile;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;

public abstract class NativeFunction {
    protected static final SymbolLookup STD_LIB = Linker.nativeLinker().defaultLookup();
    protected static final StructLayout CAPTURED_STATE_LAYOUT = Linker.Option.captureStateLayout();
    protected static final VarHandle ERRNO_HANDLE = CAPTURED_STATE_LAYOUT.varHandle(
            MemoryLayout.PathElement.groupElement("errno"));

    protected static final AddressLayout POINTER = ValueLayout.ADDRESS.withTargetLayout(
            MemoryLayout.sequenceLayout(1024, ValueLayout.JAVA_BYTE));
    protected static final MethodHandle STR_ERROR = Linker.nativeLinker().downcallHandle(
            Linker.nativeLinker().defaultLookup().find("strerror").orElseThrow(),
            FunctionDescriptor.of(POINTER, ValueLayout.JAVA_INT));
}

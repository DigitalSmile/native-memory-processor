package io.github.digitalsmile.annotation.function;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.util.Arrays;

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

    /**
     * Process the error and raise exception method.
     *
     * @param callResult    result of the call
     * @param capturedState state of errno
     * @param args arguments called to function
     * @throws NativeMemoryException if call result is -1
     */
    protected void processError(long callResult, MemorySegment capturedState, String method, Object... args) throws NativeMemoryException {
        if (callResult == -1) {
            try {
                int errno = (int) ERRNO_HANDLE.get(capturedState, 0L);
                var errnoStr = (MemorySegment) STR_ERROR.invokeExact(errno);
                throw new NativeMemoryException("Error during call to method " + method + " with data '" + Arrays.toString(args) + "': " +
                        errnoStr.getString(0) + " (" + errno + ")", errno);
            } catch (Throwable e) {
                throw new NativeMemoryException(e.getMessage(), e);
            }
        }
    }
    protected void processError(MemorySegment callResult, MemorySegment capturedState, String method, Object... args) throws NativeMemoryException {
        processError(callResult != null ? 0 : -1, capturedState, method, args);
    }

    protected void processError(MemorySegment capturedState, String method, Object... args) throws NativeMemoryException {
        try {
            int errno = (int) ERRNO_HANDLE.get(capturedState);
            if (errno > 0) {
                var errnoStr = (MemorySegment) STR_ERROR.invokeExact(errno);
                throw new NativeMemoryException("Error during call to method " + method + " with data '" + Arrays.toString(args) + "': " +
                        errnoStr.getString(0) + " (" + errno + ")", errno);
            }
        } catch (Throwable e) {
            throw new NativeMemoryException(e.getMessage(), e);
        }
    }
}

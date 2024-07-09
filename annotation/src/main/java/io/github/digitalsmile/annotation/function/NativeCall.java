package io.github.digitalsmile.annotation.function;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.util.Arrays;

/**
 * Abstract helper class to be used with generated native functions class.
 * It has static references to standard <code>LibC</code> lookup object and <code>errno/strerror</code> handles.
 * <p>
 * Can be used for processing errors from native calls.
 */
public abstract class NativeCall {
    // LibC lookup reference
    protected static final SymbolLookup STD_LIB = Linker.nativeLinker().defaultLookup();
    // Captured state for errno
    protected static final StructLayout CAPTURED_STATE_LAYOUT = Linker.Option.captureStateLayout();
    // Errno var handle
    protected static final VarHandle ERRNO_HANDLE = CAPTURED_STATE_LAYOUT.varHandle(
            MemoryLayout.PathElement.groupElement("errno"));

    private static final AddressLayout POINTER = ValueLayout.ADDRESS.withTargetLayout(
            MemoryLayout.sequenceLayout(1024, ValueLayout.JAVA_BYTE));
    // Strerror method handle
    protected static final MethodHandle STR_ERROR = Linker.nativeLinker().downcallHandle(
            Linker.nativeLinker().defaultLookup().find("strerror").orElseThrow(),
            FunctionDescriptor.of(POINTER, ValueLayout.JAVA_INT));

    /**
     * Process the error and raise exception method.
     *
     * @param callResult    result of the call
     * @param capturedState state of errno
     * @param method        string representation of called method
     * @param args          arguments called to function
     * @throws NativeMemoryException if call result is -1
     */
    protected void processError(Number callResult, MemorySegment capturedState, String method, Object... args) throws NativeMemoryException {
        if (callResult.intValue() == -1) {
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

    /**
     * Process the error and raise exception method.
     *
     * @param callResult    result of the call
     * @param capturedState state of errno
     * @param method        string representation of called method
     * @param args          arguments called to function
     * @throws NativeMemoryException if call result is -1
     */
    protected void processError(MemorySegment callResult, MemorySegment capturedState, String method, Object... args) throws NativeMemoryException {
        processError(callResult != null ? 0 : -1, capturedState, method, args);
    }

    /**
     * Process the error and raise exception method.
     *
     * @param capturedState state of errno
     * @param method        string representation of called method
     * @param args          arguments called to function
     * @throws NativeMemoryException if call result is greater than 0
     */
    protected void processError(MemorySegment capturedState, String method, Object... args) throws NativeMemoryException {
        try {
            int errno = (int) ERRNO_HANDLE.get(capturedState, 0L);
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

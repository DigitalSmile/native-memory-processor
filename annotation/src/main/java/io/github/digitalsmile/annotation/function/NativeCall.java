package io.github.digitalsmile.annotation.function;

import io.github.digitalsmile.annotation.NativeMemoryException;
import io.github.digitalsmile.annotation.types.interfaces.NativeMemoryContext;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Abstract helper class to be used with generated native functions class.
 * It has a references to context object with library, arena and methods.
 * Class provides standard <code>errno/strerror</code> handles.
 * <p>
 * Can be used for processing errors from native calls.
 */
public abstract class NativeCall implements AutoCloseable {
    protected final NativeMemoryContext context;
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

    // Available scopes
    private final static List<MemorySegment.Scope> scopes = new ArrayList<>();

    /**
     * Creates a native call instance with specified context.
     *
     * @param context context for constructing native class
     */
    protected NativeCall(NativeMemoryContext context) {
        this.context = context;
        registerScope(context.getArena().scope());
    }

    /**
     * Gets the <code>Arena</code>, backed by context.
     *
     * @return Arena instance
     */
    public Arena getInternalArena() {
        return context.getArena();
    }

    /**
     * Registers scope to be found for checking the allocated memory segments.
     * Use this method and register your scope if you are using your own <code>Arena</code> instance.
     *
     * @param scope scope to be registered
     */
    public static void registerScope(MemorySegment.Scope scope) {
        scopes.add(scope);
    }

    /**
     * Checks if current scope is registered.
     * Use this method and check your scope if you are using your own <code>Arena</code> instance.
     *
     * @param scope scope to be checked
     * @return true if the scope is registered
     */
    public static boolean createdInContext(MemorySegment.Scope scope) {
        return scopes.contains(scope);
    }

    @Override
    public void close() throws NativeMemoryException {
        try {
            this.context.close();
        } catch (UnsupportedOperationException e) {
            //Since we do not know the arena type in runtime, suppress the exception on close
        } catch (Exception e) {
            throw new NativeMemoryException(e.getMessage(), e);
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
    protected void processError(int callResult, MemorySegment capturedState, String method, Object... args) throws NativeMemoryException {
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
}

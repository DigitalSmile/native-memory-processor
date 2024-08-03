package io.github.digitalsmile.annotation.types.interfaces;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

/**
 * Interface that provides memory layout of class file for using with FFM API of recent Java versions.
 * The interface is used to provide easy access to object manipulation before handling it to native function.
 * <p>
 * It automatically adds to generated structure and unions types, parsed by <code>NativeMemory</code> annotation.
 */
public interface NativeMemoryLayout {

    /**
     * Method-helper for calling handle with specified {@link MemorySegment} to access the data behind it.
     *
     * @param handle valid method handle
     * @param buffer memory buffer, containing data
     * @return new memory segment with data to be accessed
     * @throws Throwable if any exception occurs during invokeExact call
     */
    default MemorySegment invokeExact(MethodHandle handle, MemorySegment buffer) throws Throwable {
        return (MemorySegment) handle.invokeExact(buffer, 0L);
    }

    /**
     * Gets the {@link MemoryLayout} of the structure.
     *
     * @return memory layout of the structure
     */
    MemoryLayout getMemoryLayout();

    /**
     * Converts {@link MemorySegment} buffer to a class / object structure.
     *
     * @param buffer memory segment to convert from
     * @param <T>    type of converted class / object structure
     * @return new class / object structure from a given buffer
     * @throws Throwable unchecked exception
     */
    <T extends NativeMemoryLayout & OpaqueMemoryLayout> T fromBytes(MemorySegment buffer) throws Throwable;

    /**
     * Converts a class / object structure into a {@link MemorySegment} buffer.
     *
     * @param buffer buffer to be filled with class / object structure
     * @throws Throwable unchecked exception
     */
    void toBytes(MemorySegment buffer) throws Throwable;

    /**
     * Checks if the structure is empty.
     *
     * @return true if structure is empty
     */
    boolean isEmpty();

}

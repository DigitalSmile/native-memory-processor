package io.github.digitalsmile.annotation.function;

import io.github.digitalsmile.annotation.types.interfaces.NativeMemoryLayout;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotation to be used with {@link io.github.digitalsmile.annotation.NativeMemory} to manually process functions
 * and generate FFM API ready classed. It can be used within defined interface with
 * {@link io.github.digitalsmile.annotation.structure.Structs}, {@link io.github.digitalsmile.annotation.structure.Enums}
 * and/or {@link io.github.digitalsmile.annotation.structure.Unions} annotations or completely without them.
 * Example usage:
 * <pre>
 *     {@code
 *      @NativeManualFunction(name = "ioctl", useErrno = true)
 *      int nativeCall(int fd, long command, int data) throws NativeMemoryException;
 *     }
 * </pre>
 * This code snippet will generate a Java class with all needed object to call <code>ioctl</code> native function from
 * standard <code>LibC</code> library, using <code>errno/strerror</code> for error handling.
 * Parameters of native function should be declared in interface method. The return type of the interface method can differ from return
 * type of native function, so {@link Returns} annotation should be specified on the relevant method parameter
 * in that case.
 * If native function parameter is <code>Pointer</code>, you can simply add {@link ByAddress} annotation to the
 * relevant method type parameter.
 * Example usage:
 * <pre>
 *     {@code
 *      @NativeManualFunction(name = "ioctl", useErrno = true)
 *      int nativeCall(int fd, long command, @Returns @ByAddress int data) throws NativeMemoryException;
 *     }
 * </pre>
 * <p>
 * Also, you can use generated classes from {@link io.github.digitalsmile.annotation.structure.Structs},
 * {@link io.github.digitalsmile.annotation.structure.Enums} and/or {@link io.github.digitalsmile.annotation.structure.Unions}.
 * The objects you provide to native function method call must implement
 * {@link NativeMemoryLayout} interface.
 * Example usage:
 * <pre>
 *     {@code
 *      // SomeObject generated by NativeMemory annotation and implements NativeMemoryLayout
 *      @NativeManualFunction(name = "ioctl", useErrno = true)
 *      SomeObject nativeCall(int fd, long command, @Returns @ByAddress SomeObject data) throws NativeMemoryException;
 *
 *      // You can even use generics!
 *      @NativeManualFunction(name = "ioctl", useErrno = true)
 *      <T extends NativeMemoryLayout> T nativeCall(int fd, long command, @Returns @ByAddress T data) throws NativeMemoryException;
 *
 *      // Or use generated objects as native return types!
 *      @NativeManualFunction(name = "ioctl", useErrno = true)
 *      int nativeCall(@Returns int fd, long command, @ByAddress SomeObject data) throws NativeMemoryException;}
 * </pre>
 * <p>
 * You can specify different library for load a native function by adding a <code>library</code> options. Please note,
 * that the library will be searched within standard POSIX locations (e.g. LD_LIBRARY_PATH) rather than
 * <code>java.library.path</code> (like it was in JNI).
 * If library is already loaded within classloader you can specify <code>isAlreadyLoaded</code> flag to true.
 */
@Target(value = ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NativeManualFunction {
    /**
     * The name of the native function.
     *
     * @return the name of native function
     */
    String name();

    /**
     * The library used to load symbols of native function.
     *
     * @return library which contains native function symbols
     */
    String library() default "libc";

    /**
     * Flag, indicating, that library is already loaded.
     *
     * @return true if library is already loaded
     */
    boolean isAlreadyLoaded() default false;

    /**
     * Flag to use errno/strerr for native function.
     *
     * @return true if use errno/strerr
     */
    boolean useErrno() default false;
}

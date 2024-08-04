package io.github.digitalsmile.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Options, used in context of current <code>NativeMemory</code> processing run.
 */
@Target(value = ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface NativeMemoryOptions {
    /**
     * Defines if processor should generate the separate enum file with root <code>#defines</code> or
     * constants in header file.
     * The file name will be the same as interface name.
     *
     * @return true if it is needed to generate root enum file
     */
    boolean processRootConstants() default false;

    /**
     * Defines <code>Arena</code> that should be used within the generated code.
     * Defaults to <code>Arena.ofAuto()</code> if not specified.
     *
     * @return arena type to be used
     */
    ArenaType arena() default ArenaType.AUTO;

    /**
     * Defines the <code>packageName</code> to be used for storing generated code.
     * Leave empty to have the interface package name as default.
     *
     * @return package name to be used
     */
    String packageName() default "";

    /**
     * Defines the native code include search path to be passed to <code>libclang</code>.
     * Use if you have library with complex search path.
     *
     * @return include array to be passed to <code>libclang</code>
     */
    String[] includes() default {};

    /**
     * Defines the system includes search path to be passed to <code>libclang</code>.
     * If you have issues with standard C/C++ header files, provide the path to search with this option.
     *
     * @return system include array to be passed to <code>libclang</code>
     */
    String[] systemIncludes() default {};

    /**
     * Defines if header files are system and should be parsed.
     * By default, system header processing is off to simplify the generated output and skip unused kernel/system structures.
     *
     * @return true if system headers should be parsed
     */
    boolean systemHeader() default false;

    boolean debugMode() default false;
}

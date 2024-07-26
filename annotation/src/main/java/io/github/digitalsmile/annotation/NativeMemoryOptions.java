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

    String packageName() default "";

    String[] includes() default {};
    String[] systemIncludes() default {};

    boolean debugMode() default false;
}

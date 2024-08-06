package io.github.digitalsmile.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Root annotation to mark interface with generation intent of FFM API objects.
 * This annotation is essential to start header processing, each individual interface should be annotated to make things work.
 * Before using the annotation, please add <code>annotationProcessor</code> library.
 * Simple example usage:
 * <pre>{@code
 *      @NativeMemory(header = "gpio.h")
 *      @Structs
 *      @Enums
 *      public interface Native {}
 * }</pre>
 * <br/>
 * This code snippet tells the processor to look for every structure and enum in header file <code>gpio.h</code>
 * and try to create a separate record object in Java language. The naming of files and variables will be the same
 * as in the header file, unless they are not keywords in Java (the processor will add "_" sign before reserved keyword
 * name). Header file itself can be located in <code>resources</code> folder or absolute path can be specified.
 * <br/>
 * The options, added with <code>NativeMemoryOptions</code> are context specific, hence no other <code>NativeMemory</code>
 * annotations share the same options.
 */
@Target(value = ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface NativeMemory {
    /**
     * Sets the header file path to be parsed for structures.
     * File path can be absolute or relative to <code>resources</code> folder.
     *
     * @return the header file path
     */
    String[] headers();
}

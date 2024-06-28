package io.github.digitalsmile.annotation.structure;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to be used with {@link Structs} annotation to indicate the structure to be parsed from
 * provided header file.
 * You can specify the <code>name</code> of structure to be parsed and the <code>javaName</code>,
 * which will be specified in generated file. Example usage:
 * <pre>
 *     {@code
 *          @Struct(name = "pollfd", javaName = "PollingFileDescriptor")
 * }</pre>
 */
@Target(value = ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Struct {
    /**
     * The name of structure to be parsed in header file.
     *
     * @return name of structure
     */
    String name();

    /**
     * The java name of structure to be generated in output file.
     *
     * @return java name of structure
     */
    String javaName() default "";
}

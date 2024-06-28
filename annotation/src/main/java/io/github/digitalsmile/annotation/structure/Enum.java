package io.github.digitalsmile.annotation.structure;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to be used with {@link Enums} annotation to indicate the enum to be parsed from
 * provided header file.
 * You can specify the <code>name</code> of enum to be parsed and the <code>javaName</code>,
 * which will be specified in generated file. Example usage:
 * <pre>
 *     {@code
 *          @Enum(name = "pollfd", javaName = "PollingFileDescriptor")
 * }</pre>
 */
@Target(value = ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Enum {
    /**
     * The name of enum to be parsed in header file.
     *
     * @return name of enum
     */
    String name();

    /**
     * The java name of enum to be generated in output file.
     *
     * @return java name of enum
     */
    String javaName() default "";
}

package io.github.digitalsmile.annotation.structure;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to be used with {@link Unions} annotation to indicate the unions to be parsed from
 * provided header file.
 * You can specify the <code>name</code> of union to be parsed and the <code>javaName</code>,
 * which will be specified in generated file. Example usage:
 * <pre>
 *     {@code
 *          @Union(name = "pollfd", javaName = "PollingFileDescriptor")
 * }</pre>
 */
@Target(value = ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Union {
    /**
     * The name of union to be parsed in header file.
     *
     * @return name of union
     */
    String name();

    /**
     * The java name of union to be generated in output file.
     *
     * @return java name of union
     */
    String javaName() default "";
}

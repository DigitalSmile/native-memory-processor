package io.github.digitalsmile.annotation.structure;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that is used with {@link io.github.digitalsmile.annotation.NativeMemory} annotation to mark
 * the intent of parsing enums within the header file.
 * If values are empty, every enum will be processed with original name. The name can be altered with "_" if
 * it is a reserved keyword in Java language.
 */
@Target(value = ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Enums {
    /**
     * Array of enums to be parsed. Leave empty if you need all enums in header file.
     *
     * @return array of enums to be parsed
     */
    Enum[] value() default {};
}

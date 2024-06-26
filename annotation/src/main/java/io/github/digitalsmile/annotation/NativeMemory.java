package io.github.digitalsmile.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(value = ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface NativeMemory {
    String header() default "";
    NativeMemoryOptions options() default @NativeMemoryOptions();
}

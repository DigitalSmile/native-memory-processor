package io.github.digitalsmile.annotation.library;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(value = ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface NativeMemoryLibrary {
    String name() default "libc";
    NativeFunction[] functions() default {};
}

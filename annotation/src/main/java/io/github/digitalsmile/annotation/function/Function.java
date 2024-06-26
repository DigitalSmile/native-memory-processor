package io.github.digitalsmile.annotation.function;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(value = ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Function {
    String name();
    String library() default "libc";
    boolean isAlreadyLoaded() default false;
    boolean useErrno() default false;
    Class<?> returnType() default void.class;
}

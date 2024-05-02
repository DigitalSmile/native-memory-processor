package org.digitalsmile;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Function {
    String name();
    String library() default "libc";
    boolean useErrno() default false;
    boolean useStrerror() default false;
}

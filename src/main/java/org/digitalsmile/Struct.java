package org.digitalsmile;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Struct {
    String name();
    String javaName() default "";
    boolean followComplexTypes() default false;
}

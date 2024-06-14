package org.digitalsmile;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Parameter {
    String name();
    ParameterType parameterType() default ParameterType.BY_VALUE;
}

package org.digitalsmile;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


@Retention(RetentionPolicy.RUNTIME)
public @interface NativeMemory {
    String header();
    boolean createEnumFromRootDefines() default false;
}

package io.github.digitalsmile.headers.mapping;

import com.squareup.javapoet.CodeBlock;

public interface TypeMapping {
    String typeName();
    Class<?> carrierClass();
    CodeBlock valueLayoutName();
}

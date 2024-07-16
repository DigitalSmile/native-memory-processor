package io.github.digitalsmile.headers.mapping;

import com.squareup.javapoet.CodeBlock;

import java.lang.foreign.ValueLayout;

public record ArrayOriginalType(String typeName, long arraySize, Class<?> carrierClass) implements OriginalType {

    public ArrayOriginalType(String typeName, long arraySize, ValueLayout valueLayout) {
        this(typeName, arraySize, valueLayout.carrier());
    }

    public boolean isObjectType() {
        return valueLayout().equals(ValueLayout.ADDRESS);
    }

    @Override
    public ValueLayout valueLayout() {
        if (carrierClass().equals(int.class)) {
            return ValueLayout.JAVA_INT;
        } else if (carrierClass().equals(long.class)) {
            return ValueLayout.JAVA_LONG;
        } else if (carrierClass().equals(boolean.class)) {
            return ValueLayout.JAVA_BOOLEAN;
        } else if (carrierClass().equals(double.class)) {
            return ValueLayout.JAVA_DOUBLE;
        } else if (carrierClass().equals(float.class)) {
            return ValueLayout.JAVA_FLOAT;
        } else if (carrierClass().equals(char.class)) {
            return ValueLayout.JAVA_CHAR;
        } else if (carrierClass().equals(byte.class)) {
            return ValueLayout.JAVA_BYTE;
        } else if (carrierClass().equals(short.class)) {
            return ValueLayout.JAVA_SHORT;
        } else {
            return ValueLayout.ADDRESS;
        }
    }

    public CodeBlock newArrayConstructor() {
        return CodeBlock.builder().add("new $L[]{}", valueLayout().carrier()).build();
    }


    public CodeBlock isNotArrayEmpty() {
        return CodeBlock.builder().add(".length > 0").build();
    }


    public CodeBlock isArrayEmpty() {
        return CodeBlock.builder().add(".length == 0").build();
    }

    @Override
    public String toString() {
        return carrierClass.getSimpleName() + "[" + arraySize + "]";
    }
}

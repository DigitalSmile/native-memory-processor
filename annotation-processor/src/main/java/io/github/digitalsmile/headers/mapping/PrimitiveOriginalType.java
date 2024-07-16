package io.github.digitalsmile.headers.mapping;

import com.squareup.javapoet.CodeBlock;

import java.lang.foreign.ValueLayout;

public record PrimitiveOriginalType(String typeName, ValueLayout valueLayout) implements OriginalType {

    @Override
    public Class<?> carrierClass() {
        return valueLayout.carrier();
    }

    @Override
    public String toString() {
        return valueLayout.carrier().getSimpleName();
    }

    public CodeBlock literal() {
        if (valueLayout.carrier().equals(long.class)) {
            return CodeBlock.builder().add("L").build();
        } else if (valueLayout.carrier().equals(double.class)) {
            return CodeBlock.builder().add("d").build();
        } else if (valueLayout.carrier().equals(float.class)) {
            return CodeBlock.builder().add("f").build();
        }
        return CodeBlock.builder().build();
    }

    public CodeBlock newConstructor() {
        var casting = "";
        if (valueLayout.carrier().equals(short.class)) {
            casting = "(short) ";
        } else if (valueLayout.carrier().equals(byte.class)) {
            casting = "(byte) ";
        }
        return CodeBlock.builder().add("$L", casting + "0").build();
    }



    public CodeBlock isEmpty() {
        return CodeBlock.builder().add(" == 0").build();
    }


    public CodeBlock isNotEmpty() {
        return CodeBlock.builder().add(" > 0").build();
    }


}

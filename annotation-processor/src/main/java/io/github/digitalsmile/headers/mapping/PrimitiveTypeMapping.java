package io.github.digitalsmile.headers.mapping;

import com.squareup.javapoet.CodeBlock;

import java.lang.foreign.AddressLayout;
import java.lang.foreign.ValueLayout;

public record PrimitiveTypeMapping(ValueLayout valueLayout) implements TypeMapping {


    @Override
    public String typeName() {
        return valueLayout != null ? valueLayout.carrier().getSimpleName() : "void";
    }

    @Override
    public Class<?> carrierClass() {
        return valueLayout.carrier();
    }

    @Override
    public CodeBlock valueLayoutName() {
        switch (valueLayout()) {
            case ValueLayout.OfBoolean _ -> {
                return CodeBlock.builder().add("JAVA_BOOLEAN").build();
            }
            case AddressLayout _ -> {
                return CodeBlock.builder().add("JAVA_ADDRESS").build();
            }
            case ValueLayout.OfByte _ -> {
                return CodeBlock.builder().add("JAVA_BYTE").build();
            }
            case ValueLayout.OfChar _ -> {
                return CodeBlock.builder().add("JAVA_CHAR").build();
            }
            case ValueLayout.OfDouble _ -> {
                return CodeBlock.builder().add("JAVA_DOUBLE").build();
            }
            case ValueLayout.OfFloat _ -> {
                return CodeBlock.builder().add("JAVA_FLOAT").build();
            }
            case ValueLayout.OfInt _ -> {
                return CodeBlock.builder().add("JAVA_INT").build();
            }
            case ValueLayout.OfLong _ -> {
                return CodeBlock.builder().add("JAVA_LONG").build();
            }
            case ValueLayout.OfShort _ -> {
                return CodeBlock.builder().add("JAVA_SHORT").build();
            }
            case null -> {
                return CodeBlock.builder().add("").build();
            }
        }
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

    public CodeBlock newArrayConstructor() {
        return CodeBlock.builder().add("new $L[]{}", valueLayout.carrier()).build();
    }

    public CodeBlock isEmpty() {
        return CodeBlock.builder().add(" == 0").build();
    }


    public CodeBlock isNotEmpty() {
        return CodeBlock.builder().add(" > 0").build();
    }


    public CodeBlock isNotArrayEmpty() {
        return CodeBlock.builder().add(".length > 0").build();
    }


    public CodeBlock isArrayEmpty() {
        return CodeBlock.builder().add(".length == 0").build();
    }
}

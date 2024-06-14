package org.digitalsmile.parser;

import com.squareup.javapoet.CodeBlock;
import org.openjdk.jextract.Type;

import java.lang.foreign.AddressLayout;
import java.lang.foreign.ValueLayout;
import java.util.List;

public record TypeMapping(List<Type.Primitive.Kind> types, ValueLayout valueLayout) {

    public TypeMapping(Type.Primitive.Kind type, ValueLayout valueLayout) {
        this(List.of(type), valueLayout);
    }

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
        }
    }

    public Class<?> carrierClass() {
        return valueLayout.carrier();
    }

    public Class<?> arrayClass() {
        return valueLayout.carrier().arrayType();
    }

    public CodeBlock newConstructor() {
        return CodeBlock.builder().add("$L", valueLayout.carrier().equals(short.class) ? "(short) 0" : "0").build();
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

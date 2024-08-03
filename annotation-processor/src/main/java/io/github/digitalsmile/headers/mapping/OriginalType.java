package io.github.digitalsmile.headers.mapping;

import com.squareup.javapoet.CodeBlock;
import org.openjdk.jextract.Type;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.lang.foreign.AddressLayout;
import java.lang.foreign.ValueLayout;
import java.util.List;
import java.util.Map;

import static org.openjdk.jextract.Type.Primitive.Kind.*;

public interface OriginalType {
    String typeName();

    Class<?> carrierClass();

    ValueLayout valueLayout();

    default CodeBlock valueLayoutName() {
        switch (valueLayout()) {
            case ValueLayout.OfBoolean _ -> {
                return CodeBlock.builder().add("JAVA_BOOLEAN").build();
            }
            case AddressLayout _ -> {
                return CodeBlock.builder().add("ADDRESS").build();
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

    Map<List<Type.Primitive.Kind>, ValueLayout> C_PRIMITIVE_MAPPING = Map.of(
            List.of(Int), ValueLayout.JAVA_INT,
            List.of(Char, Char16), ValueLayout.JAVA_BYTE,
            List.of(Long, LongLong), ValueLayout.JAVA_LONG,
            List.of(Short), ValueLayout.JAVA_SHORT,
            List.of(Float), ValueLayout.JAVA_FLOAT,
            List.of(Double, LongDouble), ValueLayout.JAVA_DOUBLE,
            List.of(Bool), ValueLayout.JAVA_BOOLEAN,
            List.of(Void), ValueLayout.ADDRESS
    );


    Map<TypeKind, ValueLayout> JAVA_PRIMITIVE_MAPPING = Map.of(
            TypeKind.BOOLEAN, ValueLayout.JAVA_BOOLEAN,
            TypeKind.BYTE, ValueLayout.JAVA_BYTE,
            TypeKind.CHAR, ValueLayout.JAVA_BYTE,
            TypeKind.DOUBLE, ValueLayout.JAVA_DOUBLE,
            TypeKind.FLOAT, ValueLayout.JAVA_FLOAT,
            TypeKind.INT, ValueLayout.JAVA_INT,
            TypeKind.LONG, ValueLayout.JAVA_LONG,
            TypeKind.SHORT, ValueLayout.JAVA_SHORT,
            TypeKind.DECLARED, ValueLayout.ADDRESS
    );

    private static ValueLayout find(TypeKind kind) {
        return JAVA_PRIMITIVE_MAPPING.entrySet().stream().filter(e -> e.getKey().equals(kind)).findFirst().orElseThrow(() -> new RuntimeException("Unsupported primitive kind: " + kind)).getValue();
    }

    private static ValueLayout find(Type.Primitive.Kind kind) {
        return C_PRIMITIVE_MAPPING.entrySet().stream().filter(e -> e.getKey().contains(kind)).findFirst().orElseThrow(() -> new RuntimeException("Unsupported primitive kind: " + kind)).getValue();
    }

    static OriginalType ofArray(OriginalType originalType, long size) {
        return new ArrayOriginalType(originalType.typeName(), size, originalType.carrierClass());
    }

    static OriginalType ofObject(String typeName) {
        return new ObjectOriginalType(typeName);
    }

    static OriginalType of(TypeMirror typeMirror) {
        var kind = typeMirror.getKind();
        if (kind.isPrimitive()) {
            var type = find(kind);
            return new PrimitiveOriginalType(type.carrier().getSimpleName(), type);
        } else if (kind.equals(TypeKind.VOID)) {
            return new ObjectOriginalType(void.class.getSimpleName());
        } else if (kind.equals(TypeKind.ARRAY)) {
            var arrayType = (ArrayType) typeMirror;
            var componentType = arrayType.getComponentType();
            if (componentType.getKind().equals(TypeKind.DECLARED)) {
                return new ArrayOriginalType(arrayType.toString(), 0, of(componentType).carrierClass());
            } else {
                return new ArrayOriginalType(arrayType.toString(), 0, find(componentType.getKind()));
            }
        }
        var name = typeMirror.toString();
        return ofObject(name);
    }

    static OriginalType of(Type type) {
        switch (type) {
            case Type.Array typeArray -> {
                var originalType = of(typeArray.elementType());
                return ofArray(originalType, typeArray.elementCount().orElse(0));
            }
            case Type.Declared typeDeclared -> {
                return ofObject(typeDeclared.tree().name());
            }
            case Type.Delegated typeDelegated -> {
                return of(typeDelegated.type());
            }
            case Type.Primitive typePrimitive -> {
                return new PrimitiveOriginalType(typePrimitive.kind().typeName(), find(typePrimitive.kind()));
            }
            case Type.Function _ -> {
                return new FunctionOriginalType();
            }
            default -> {
                throw new IllegalArgumentException("Type " + type + " is not supported");
            }
        }
    }
}

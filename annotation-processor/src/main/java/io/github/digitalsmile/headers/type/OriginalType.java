package io.github.digitalsmile.headers.type;

import io.github.digitalsmile.headers.mapping.DeferredObjectTypeMapping;
import io.github.digitalsmile.headers.mapping.ObjectTypeMapping;
import io.github.digitalsmile.headers.mapping.PrimitiveTypeMapping;
import io.github.digitalsmile.headers.mapping.TypeMapping;
import org.openjdk.jextract.Type;

import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.openjdk.jextract.Type.Primitive.Kind.*;

public interface OriginalType {
    String typeName();

    <T extends TypeMapping> T typeMapping();

    Map<List<Type.Primitive.Kind>, PrimitiveTypeMapping> C_PRIMITIVE_MAPPING = Map.of(
            List.of(Int), new PrimitiveTypeMapping(ValueLayout.JAVA_INT),
            List.of(Char, Char16), new PrimitiveTypeMapping(ValueLayout.JAVA_BYTE),
            List.of(Long, LongLong), new PrimitiveTypeMapping(ValueLayout.JAVA_LONG),
            List.of(Short), new PrimitiveTypeMapping(ValueLayout.JAVA_SHORT),
            List.of(Float), new PrimitiveTypeMapping(ValueLayout.JAVA_FLOAT),
            List.of(Double, LongDouble), new PrimitiveTypeMapping(ValueLayout.JAVA_DOUBLE),
            List.of(Bool), new PrimitiveTypeMapping(ValueLayout.JAVA_BOOLEAN),
            List.of(Void), new PrimitiveTypeMapping(ValueLayout.JAVA_LONG)
    );


    Map<TypeKind, PrimitiveTypeMapping> JAVA_PRIMITIVE_MAPPING = Map.of(
            TypeKind.BOOLEAN, new PrimitiveTypeMapping(ValueLayout.JAVA_BOOLEAN),
            TypeKind.BYTE, new PrimitiveTypeMapping(ValueLayout.JAVA_BYTE),
            TypeKind.CHAR, new PrimitiveTypeMapping(ValueLayout.JAVA_BYTE),
            TypeKind.DOUBLE, new PrimitiveTypeMapping(ValueLayout.JAVA_DOUBLE),
            TypeKind.FLOAT, new PrimitiveTypeMapping(ValueLayout.JAVA_FLOAT),
            TypeKind.INT, new PrimitiveTypeMapping(ValueLayout.JAVA_INT),
            TypeKind.LONG, new PrimitiveTypeMapping(ValueLayout.JAVA_LONG),
            TypeKind.SHORT, new PrimitiveTypeMapping(ValueLayout.JAVA_SHORT)
    );

    private static PrimitiveTypeMapping find(TypeKind kind) {
        return JAVA_PRIMITIVE_MAPPING.entrySet().stream().filter(e -> e.getKey().equals(kind)).findFirst().orElseThrow(() -> new RuntimeException("Unsupported primitive kind: " + kind)).getValue();
    }

    private static PrimitiveTypeMapping find(Type.Primitive.Kind kind) {
        return C_PRIMITIVE_MAPPING.entrySet().stream().filter(e -> e.getKey().contains(kind)).findFirst().orElseThrow(() -> new RuntimeException("Unsupported primitive kind: " + kind)).getValue();
    }

    List<ObjectTypeMapping> OBJECT_MAPPING = new ArrayList<>();
    List<TypeMapping> DEFERRED_OBJECT_MAPPING = new ArrayList<>();

    static void register(ObjectTypeMapping objectTypeMapping) {
        OBJECT_MAPPING.add(objectTypeMapping);
    }

    static void resolve(String typeName) {
        var typeMapping = DEFERRED_OBJECT_MAPPING.stream().filter(t -> t.typeName().equals(typeName)).findFirst();
        if (typeMapping.isPresent()) {
            var deferredType = typeMapping.get();
            DEFERRED_OBJECT_MAPPING.remove(deferredType);
            deferredType = new ObjectTypeMapping(typeName);
            register((ObjectTypeMapping) deferredType);
        }
    }

    private static TypeMapping find(String typeName) {
        var typeMapping = OBJECT_MAPPING.stream().filter(t -> t.typeName().equals(typeName)).findFirst();
        if (typeMapping.isPresent()) {
            return typeMapping.get();
        } else {
            var deferred = new DeferredObjectTypeMapping(typeName);
            DEFERRED_OBJECT_MAPPING.add(deferred);
            return deferred;
        }
    }

    static OriginalType ofArray(OriginalType originalType, long size) {
        return new ArrayOriginalType(originalType.typeName(), size, originalType.typeMapping());
    }

    static OriginalType ofObject(String typeName) {
        return new ObjectOriginalType(typeName, find(typeName));
    }

    static OriginalType of(TypeMirror typeMirror) {
        var kind = typeMirror.getKind();
        if (kind.isPrimitive()) {
            var type = find(kind);
            return new PrimitiveOriginalType(type.typeName(), type);
        } else if (kind.equals(TypeKind.VOID)) {
            return new ObjectOriginalType("void", new ObjectTypeMapping("void", void.class, false));
        }
        return ofObject(typeMirror.toString());
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

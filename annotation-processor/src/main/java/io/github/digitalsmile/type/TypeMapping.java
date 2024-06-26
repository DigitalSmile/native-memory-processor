package io.github.digitalsmile.type;

import com.squareup.javapoet.CodeBlock;
import org.openjdk.jextract.Type;

import java.util.List;

public interface TypeMapping {
    Class<?> carrierClass();
    Class<?> arrayClass();
    CodeBlock valueLayoutName();
    CodeBlock newConstructor();
    CodeBlock newArrayConstructor();
    CodeBlock isEmpty();
    CodeBlock isNotEmpty();
    CodeBlock isNotArrayEmpty();
    CodeBlock isArrayEmpty();
    List<Type.Primitive.Kind> types();
}

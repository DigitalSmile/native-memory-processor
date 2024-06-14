package org.digitalsmile.parser;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import jdk.jshell.spi.ExecutionControl;
import org.openjdk.jextract.Type;

import javax.lang.model.element.Modifier;
import java.security.cert.CertificateRevokedException;
import java.util.ArrayList;
import java.util.List;

import static org.digitalsmile.parser.StructComposer.typeMapping;

public class EnumComposer {

    public static String compose(NativeMemoryModel nativeMemoryModel, String packageName, String prettyName) {
        var enumBuilder = TypeSpec.enumBuilder(prettyName)
                .addModifiers(Modifier.PUBLIC);
        TypeMapping typeMapping = null;
        for (NativeMemoryNode node : nativeMemoryModel.getNodes()) {
            if (node.getType() instanceof Type.Primitive typePrimitive) {
                if (typeMapping == null) {
                    typeMapping = findTypeByValueClass(node.getValue().getClass());
                }
                enumBuilder.addEnumConstant(node.getName(), TypeSpec.anonymousClassBuilder("$LL", node.getValue()).build());
            } else {
                System.err.println(node.getName() + ": only primitive types are allowed in enum");
            }
        }

        var outputFile = JavaFile.builder(packageName,
                enumBuilder
                        .addField(typeMapping.carrierClass(), "value", Modifier.PRIVATE, Modifier.FINAL)
                        .addMethod(MethodSpec.constructorBuilder()
                                .addParameter(typeMapping.carrierClass(), "value")
                                .addStatement("this.$N = $N", "value", "value")
                                .build())
                        .build()
        ).indent("\t").skipJavaLangImports(true).build();
        return outputFile.toString();
    }

    private static TypeMapping findType(Type.Primitive.Kind type) {
        return typeMapping.stream().filter(list -> list.types().contains(type)).findFirst().orElseThrow();
    }

    private static TypeMapping findTypeByValueClass(Class<?> clazz) {
        Object primitiveType;
        try {
            primitiveType = clazz.getField("TYPE").get(null);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        return typeMapping.stream().filter(list -> {
            return list.carrierClass().equals(primitiveType);
        }).findFirst().orElseThrow();
    }
}

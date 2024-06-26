package io.github.digitalsmile.composers;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import io.github.digitalsmile.parser.NativeMemoryModel;
import io.github.digitalsmile.parser.NativeMemoryNode;
import io.github.digitalsmile.type.TypeMapping;
import org.openjdk.jextract.Type;

import javax.lang.model.element.Modifier;

import static io.github.digitalsmile.composers.StructComposer.PRIMITIVE_MAPPING;

public class EnumComposer {

    public static String compose(NativeMemoryModel nativeMemoryModel, String packageName, String prettyName) {
        var enumBuilder = TypeSpec.enumBuilder(prettyName)
                .addModifiers(Modifier.PUBLIC);
        TypeMapping primitiveMapping = null;
        for (NativeMemoryNode node : nativeMemoryModel.getNodes()) {
            if (node.getType() instanceof Type.Primitive typePrimitive) {
                if (primitiveMapping == null) {
                    primitiveMapping = findTypeByValueClass(node.getValue().getClass());
                }
                enumBuilder.addEnumConstant(node.getName(), TypeSpec.anonymousClassBuilder("$LL", node.getValue()).build());
            } else {
                System.err.println(node.getName() + ": only primitive types are allowed in enum");
            }
        }

        var outputFile = JavaFile.builder(packageName,
                enumBuilder
                        .addField(primitiveMapping.carrierClass(), "value", Modifier.PRIVATE, Modifier.FINAL)
                        .addMethod(MethodSpec.constructorBuilder()
                                .addParameter(primitiveMapping.carrierClass(), "value")
                                .addStatement("this.$N = $N", "value", "value")
                                .build())
                        .build()
        ).indent("\t").skipJavaLangImports(true).build();
        return outputFile.toString();
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
        return PRIMITIVE_MAPPING.stream().filter(list -> {
            return list.carrierClass().equals(primitiveType);
        }).findFirst().orElseThrow();
    }
}

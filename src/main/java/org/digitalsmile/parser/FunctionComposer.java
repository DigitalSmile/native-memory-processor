package org.digitalsmile.parser;

import com.squareup.javapoet.*;
import org.digitalsmile.*;
import org.digitalsmile.Enum;
import org.openjdk.jextract.Type;

import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.digitalsmile.parser.StructComposer.typeMapping;

public class FunctionComposer {

    public static String compose(String packageName, String javaName, List<Element> elements, Types typeUtils) {
        var classBuilder = TypeSpec.classBuilder(javaName + "Impl")
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(ClassName.get(packageName, javaName))
                .superclass((ClassName.get(packageName, "NativeFunction")));
        var methodCount = 0;

        for (Element element : elements) {
            methodCount++;
            var method = (ExecutableElement) element;
            var functionAnnotation = element.getAnnotation(Function.class);

            List<CodeBlock> parameters = new ArrayList<>();
            //var type = findType(method.getReturnType());
            var type = findType(functionAnnotation.returnType());
            parameters.add(CodeBlock.builder().add("$T.$L", ValueLayout.class, type != null ? type.valueLayoutName() : "ADDRESS").build());

            for (VariableElement variableElement : method.getParameters()) {
                var parameterType = Arrays.stream(functionAnnotation.parameters())
                        .filter(parameter -> parameter.name().equals(variableElement.getSimpleName().toString()))
                        .map(Parameter::parameterType).findFirst().orElse(ParameterType.BY_VALUE);
                if (parameterType.equals(ParameterType.BY_ADDRESS)) {
                    parameters.add(CodeBlock.builder().add("$T.ADDRESS", ValueLayout.class).build());
                } else {
                    var elementType = findType(variableElement.asType());
                    parameters.add(CodeBlock.builder().add("$T.$L", ValueLayout.class, elementType != null ? elementType.valueLayoutName() : "ADDRESS").build());
                }
            }
            classBuilder.addMethod(MethodSpec.overriding(method).addStatement("return null").build())
                    .addField(FieldSpec.builder(MethodHandle.class, functionAnnotation.name().toUpperCase() + methodCount,
                                    Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                            .initializer(CodeBlock.builder()
                                    .add("$T.nativeLinker().downcallHandle($W",
                                            Linker.class)
                                    .add("STD_LIB.find($S).orElseThrow(),$W", functionAnnotation.name())
                                    .add("$T.of($L),$W", FunctionDescriptor.class, CodeBlock.join(parameters, ", "))
                                    .add("$T.captureCallState($S))", Linker.Option.class, "errno")
                                    .build())
                            .build());
        }

        var outputFile = JavaFile.builder(packageName, classBuilder.build()).indent("\t").skipJavaLangImports(true).build();
        return outputFile.toString();
    }

    private static TypeMapping findType(String type) {
        return typeMapping.stream().filter(list -> list.carrierClass().getSimpleName().equals(type)).findFirst().orElse(null);
    }
    private static TypeMapping findType(TypeMirror type) {
        return typeMapping.stream().filter(list -> list.carrierClass().getSimpleName().equals(type.toString())).findFirst().orElse(null);
    }


//    @NativeMemory(header = "/usr/src/linux-headers-6.2.0-39/include/uapi/linux/gpio.h")
//    @Structs({
//            @Struct(name = "gpiochip_info", javaName = "ChipInfo"),
//            @Struct(name = "gpio_v2_line_info", javaName = "LineInfo"),
//            @Struct(name = "gpio_v2_line_attribute", javaName = "LineAttribute"),
//            @Struct(name = "gpio_v2_line_config", javaName = "LineConfig"),
//            @Struct(name = "gpio_v2_line_request", javaName = "LineRequest"),
//            @Struct(name = "gpio_v2_line_values", javaName = "LineValues"),
//            @Struct(name = "gpio_v2_line_config_attribute", javaName = "LineConfigAttribute"),
//            @Struct(name = "gpio_v2_line_flag", javaName = "LineFlag")
//    })
//    @Enums(value = {
//            @org.digitalsmile.Enum(name = "gpio_v2_line_event_id", javaName = "LineEventId"),
//            @org.digitalsmile.Enum(name = "gpio_v2_line_attr_id", javaName = "LineAttrId"),
//            @Enum(name = "gpio_v2_line_flag", javaName = "LineFlag")
//    })
//    public interface Ioctl {
//        @Function(name = "ioctl", useErrno = true, useStrerror = true, returnType = "int")
//        long call(int fd, long command, long data);
//
//        @Function(name = "ioctl", useErrno = true, useStrerror = true, returnParameter = "data",
//                parameters = {
//                        @Parameter(name = "data", parameterType = ParameterType.BY_ADDRESS)
//                }, returnType = "int")
//        <T extends NativeMemoryLayout> T call(int fd, long command, T data);
//    }

}

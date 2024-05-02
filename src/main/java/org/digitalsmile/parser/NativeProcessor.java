package org.digitalsmile.parser;


import org.digitalsmile.Native;
import org.digitalsmile.Struct;
import org.digitalsmile.Structs;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;

@SupportedSourceVersion(SourceVersion.RELEASE_22)
public class NativeProcessor extends AbstractProcessor {

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(Native.class.getName(), Structs.class.getName(), Struct.class.getName());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            return false;
        }
//        StringBuilder finalDescriptor = new StringBuilder()
//                .append("<?xml version=\"1.0\" encoding=\"utf-8\" ?>")
//                .append("\n<deployable-unit>\n")
//                .append("handlersXml")
//                .append("\n</deployable-unit>\n");
//
//        try {
//            FileObject file = processingEnv.getFiler().createResource(StandardLocation.SOURCE_OUTPUT, "resources", "descriptor.xml");
//            Writer writer = file.openWriter();
//            writer.write(finalDescriptor.toString());
//            writer.close();
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
        for (Element element : roundEnv.getElementsAnnotatedWith(Native.class)) {
            var nativeAnnotation = element.getAnnotation(Native.class);
            var headerFile = nativeAnnotation.header();
            var header = Path.of(headerFile);
            if (!header.toFile().exists()) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Cannot find header file '" + headerFile + "' annotated in class '" + element + "'!");
                return false;
            }
            var structs = element.getAnnotation(Structs.class);
            for (Struct struct : structs.value()) {
                var parser = new Parser(processingEnv);
                var packageName = processingEnv.getElementUtils().getPackageOf(element).getQualifiedName().toString();
                try {
                    parser.parse(header, struct.name(), struct.javaName(), packageName);
                } catch (IOException e) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
                }
            }

        }

        return true;
    }
}

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
import java.io.Writer;
import java.util.Set;

@SupportedSourceVersion(SourceVersion.RELEASE_22)
public class NativeProcessor extends AbstractProcessor {

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(Native.class.getName(), Structs.class.getName(), Struct.class.getName());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
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
//        if (roundEnv.processingOver()) {
//            return false;
//        }
        for (Element element : roundEnv.getRootElements()) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, element.toString());
        }

        return false;
    }
}

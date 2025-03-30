module annotation.processor.main {
    requires io.avaje.prism;
    requires io.github.digitalsmile.annotation;
    requires java.compiler;

    requires com.squareup.javapoet;
    requires org.barfuin.texttree;

    provides javax.annotation.processing.Processor with io.github.digitalsmile.NativeProcessor;
}
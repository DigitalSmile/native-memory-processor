/**
 * Module Native Annotation
 */
module io.github.digitalsmile.annotation {
    // structure, enum and union annotations and helpers
    opens io.github.digitalsmile.annotation.structure;
    // function annotation and helpers
    opens io.github.digitalsmile.annotation.function;
    // base NativeMemory annotation class
    opens io.github.digitalsmile.annotation;

    exports io.github.digitalsmile.annotation;
    exports io.github.digitalsmile.annotation.function;
    exports io.github.digitalsmile.annotation.structure;
    exports io.github.digitalsmile.annotation.types;
    opens io.github.digitalsmile.annotation.types;
    exports io.github.digitalsmile.annotation.types.interfaces;
    opens io.github.digitalsmile.annotation.types.interfaces;
    exports io.github.digitalsmile.annotation.library;
    opens io.github.digitalsmile.annotation.library;
}
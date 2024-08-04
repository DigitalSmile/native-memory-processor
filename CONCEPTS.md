# Concepts

The main concept behind the library is to provide easy access to native structures and functions of dynamic libraries written in different languages.
Java FFM API provides solid core objects and patterns to interact with native code, but is lacking of DevEx when working with real native code.
That said, the developer needs to write a lot of boilerplate code to make s simple native call and understand the whole native-java engine under the hood.

To solve this issue `native-memory-processor` consists of two major parts:
1) `annotation` project, which declares core annotations and some helpers to work with native code in runtime.
    - `@NativeMemory` - main annotation to interface. Provide header files for parsing or leave empty if you need just native function generation.
    - `@NativeMemoryOptions` - annotation to interface, can define optional advanced options to code generation.
    - `@Structs` / `@Struct` - annotations to interface, defines the intent to parse structures.
    - `@Enums` / `@Enum` - annotations to interface, defines the intent to parse enums.
    - `@Unions` / `@Union` - annotations to interface, defines the intent to parse unions.
    - `@NativeManualFunction` - annotation to method in interface, defines the native function to be generated.
    - `@ByAddress` / `@Returns` - annotation to parameter of method in interface, defines parameter option to send as a pointer / value and native function return object.

2) `annotation-processor` project, which provides processing of annotation described above at compile time in few steps:
    - indicate the main `@NativeMemory` annotation on interface
    - get provided header files (if any) and generate structures/enums/unions as defined by corresponding annotations
    - get annotated by `@NativeManualFunction` methods in interface and generate java-to-native code

In general, library tries to encapsulate the hardcore part of native interacting by code generation and provide user-specific classes with variety of options.

## Difference from jextract

The OpenJDK team had already created a similar tool, called `jextract` to keep developer hands clean from native code.
This is a standalone CLI tool, which works with `libclang` to parse provided header files and generate the Java FFM-ready code.
While it is great in concept, I found it 'not-so-friendly' for a person, who never worked with native code before.
Mainly, the interacting and generated code is hard to understand in different ways:
- structure/function names usually uses `_` and `$` signs or its combinations in generated methods
- no support of opaque structures
- it is HUGE! Simple `gpiochip_info` structure from `gpio.h` kernel UAPI of three fields transforms to 284 lines of code (sic!)
- jextract holds no context of what it is generating. Meaning you can get a header file for defining primitive types by kernel (e.g. `__kernel_sighandler_t` for above structure), which is usually do not needed in your code
- there is no simple way to understand the connections between generated classes, because of it's size and naming
- it is standalone binary, which is needed to be connected somehow to your gradle/maven build toolchain

I tried to get all advantages of `jextract` and create more user-friendly version of it.
Still both `native-memory-processor` and `jextract` uses the same parsing library `libclang` and the quality of parsers are the same, but the process is very different:
- annotation processing by gradle/maven instead of standalone binary
- very specific control over the objects you are parsing with context based approach
- more helpers and code validation
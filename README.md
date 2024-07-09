# Java Native Memory Processing
![](https://img.shields.io/badge/Java-22+-success)
![Maven Central Version](https://img.shields.io/maven-central/v/io.github.digitalsmile.native/annotation?label=annotation)
![Maven Central Version](https://img.shields.io/maven-central/v/io.github.digitalsmile.native/annotation-processor?label=annotation-processor)
![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/digitalsmile/native-memory-processor/gradle.yml)

## Introduction

With the release of JDK 22 the new Foreign Function & Memory API (FFM API) has been introduced from preview phase.

This API is intended to substitute Java Native Interface (JNI) and become a standard of interacting with native code from Java.

While FFM API itself is very powerful and can provide access to onheap/offheap memory segments and call a native function, it is lacking of extended features of converting C/C++ structures to Java classes. 
Of course, we have `jextract` tool, but the code it generates is kind of messy and hard to understand.

This project goal is to combine the power of FFM API and Java, like annotation processing, to make you happy while working with native functions and structures.

## Features
- two separate libraries - `annotation` and `annotation-processor` working with code generation during compile time
- C/C++ types support in top level: struct, union, enum
- can load third party native libraries or use already loaded by classloader
- flexible native-to-java function declarations
- pass to native functions primitives and objects, by value or just a pointer
- java exceptions with the power of errno/strerr 
- safe and clean code generation
- useful helpers to work with structures, like `.createEmpty()`
- ready to write your project unit tests with generated native structures

## Requirements

- `JDK22+` with FFM API enabled ( use `--enable-native-access=ALL-UNNAMED` run parameter or add `Enable-Native-Access: ALL-UNNAMED` to jar manifest file)
- `glibc 2.31+` (ubuntu 20.04+ or MacOS Sonoma+) on host machine where annotation processor is running (requirement for `libclang`). 
Windows hosts are not yet supported.

## Quick start

1) Add dependencies to your `build.gradle`:
```groovy
dependencies {
    // Annotations to use for code generation
    implementation 'io.github.digitalsmile.native:annotation:{$version}'
    // Process annotations and generate code at compile time
    annotationProcessor 'io.github.digitalsmile.native:annotation-processor:{$version}'
}
```

2) Define interface class and run your build:
```java
@NativeMemory(header = "gpio.h")
@Structs
@Enums
public interface GPIO {
    @NativeFunction(name = "ioctl", useErrno = true, returnType = int.class)
    int nativeCall(int fd, long command, int data) throws NativeMemoryException;
}
```
This code snippet will generate all structures and enums within the header file `gpio.h` (located in `resources` folder), as well as `GPIONative` class with call implementation of `ioctl` native function.
Find more examples in [documentation](USAGE.md) or in my other project https://github.com/digitalsmile/gpio 

3) Enjoy! :)

## Plans

- more support of native C/C++ types and patterns
- validation of structures parameters and custom validation support
- adding more context on different header files, especially connected with each other


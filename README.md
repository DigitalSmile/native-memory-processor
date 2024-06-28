# Java Native Memory Processing
![](https://img.shields.io/badge/Java-22+-success)
![Maven Central Version](https://img.shields.io/maven-central/v/io.github.digitalsmile.native/annotation?color=blue&link=https%3A%2F%2Fcentral.sonatype.com%2Fartifact%2Fio.github.digitalsmile.native%2Fannotation)
![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/digitalsmile/annotation/gradle.yml)
## Introduction
With the release of JDK 22 the new Foreign Function & Memory API (FFM API) has been introduced from preview phase.

This API is intended to substitute Java Native Interface (JNI) and become a standard of interacting with native code from Java.

While FFM API itself is very powerful and can provide access to onheap/offheap memory regions and call a native function, it is lacking of extended features of converting C/C++ structures to Java classes.Of course, we have `jextract` tool, but the code it generates is kind of messy and hard to understand.

This project goal is to combine the power of FFM API and some Java patterns, like annotation and annotation processing, to make you happy while working with native functions and structures.
## Features
- two separate libraries - `annotation` and `annotation-processor` working with code generation during compile time
- C/C++ types support in top level: struct, union, enum
- can load third party libraries or use already loaded by classloader
- flexible native-to-java method declarations
- pass to native functions primitives and objects, by value or just a pointer
- java exceptions with the power of errno/strerr 
- safe and clean code generation
- useful helpers to work with structures, like `.createEmpty()`
- ready to write your project unit tests with generated structures
## Getting started
1) Ensure you are working with JDK22+.
2) To enable FFM API access use `--enable-native-access=ALL-UNNAMED` run parameter or add `Enable-Native-Access: ALL-UNNAMED` to jar manifest file.
3) Add dependencies to your `build.gradle`:
```groovy
dependencies {
    annotationProcessor 'io.github.digitalsmile.native:annotation-processor:{$version}'
    implementation 'io.github.digitalsmile.native:annotation:{$version}'
}
```
4) Define interface class and run your build:
```java
@NativeMemory(header = "gpio.h")
@Structs
@Enums
public interface GPIO {
    @Function(name = "ioctl", useErrno = true, returnType = int.class)
    int nativeCall(int fd, long command, int data) throws NativeMemoryException;
}
```
This code snippet will generate all structures and enums within the header file `gpio.h` (located in `resources` folder), as well as `GPIONative` class with call implementation of `ioctl` native function.
Find more examples in javadoc or in my other project https://github.com/digitalsmile/gpio 

5) Enjoy! :)
## Plans
- more support of native C/C++ types and patterns
- adding `libclang` as a separate transient dependency
- validation of structures parameters and custom validation support
- adding more context on different header files, especially connected with each other
- adding javadoc in code generation classes


# Getting started

## Quick Start

1) Locate your library header files. Usually, it is a single header file, which interconnects all needed structures/classes. E.g.
```shell
git clone --depth 1 https://github.com/curl/curl.git
cd curl/include/curl
pwd
ls curl.h
```
2) Observe the header file and choose what structures/enums/functions you need.
3) Create an interface `Libcurl` in your java project and locate your `systemIncludes` (or skip if you have it in your 
system path). Fill in the header path and selected structures as follows:
```java
@NativeMemory(headers = "libcurl/curl/include/curl/curl.h")
@NativeMemoryOptions(systemIncludes = {
        "/usr/lib/gcc/x86_64-linux-gnu/12/include/"
    }, processRootConstants = true)
@Structs({
    @Struct(name = "CURL", javaName = "CurlInstance")
})
@Enums({
    @Enum(name = "CURLcode", javaName = "CURLCode"),
    @Enum(name = "CURLoption", javaName = "CURLOption")
})
public interface Libcurl {}
```
4) Run build and observe the generated classes in project `build/generated/sources/annotationProcessor/java/main/{you-package-name}`.
The package name defaults to where the `Libcurl` interface created.
5) Locate you dynamic library `libcurl.so` (or `libcurl.dylib`). You can use precompiled binary or compile it by yourself.
6) Set up the native functions mapping (this is a minimum to run hello world example):
```java
public interface Libcurl {

    @NativeManualFunction(name = "curl_easy_init", library = "/usr/lib/x86_64-linux-gnu/libcurl.so")
    CURL easyInit() throws NativeMemoryException;

    @NativeManualFunction(name = "curl_global_init", library = "/usr/lib/x86_64-linux-gnu/libcurl.so")
    CURLcode globalInit(long flags) throws NativeMemoryException;

    @NativeManualFunction(name = "curl_easy_setopt", library = "/usr/lib/x86_64-linux-gnu/libcurl.so")
    CURLcode easySetOpt(CURL curl, CURLoption option, String value) throws NativeMemoryException;

    @NativeManualFunction(name = "curl_easy_setopt", library = "/usr/lib/x86_64-linux-gnu/libcurl.so")
    CURLcode easySetOpt(CURL curl, CURLoption option, @ByAddress long value) throws NativeMemoryException;

    @NativeManualFunction(name = "curl_easy_perform", library = "/usr/lib/x86_64-linux-gnu/libcurl.so")
    CURLcode easyPerform(CURL curl) throws NativeMemoryException;
}
```
8) Run build and observe `LibcurlNative` class at the same location.
7) Write your example method
```java
    public static void main(String[] args) throws NativeMemoryException {
        try(var libcurl = new LibcurlNative()) {
            var code = libcurl.globalInit(CurlConstants.CURL_GLOBAL_DEFAULT);
            assertEquals(code, CURLcode.CURLE_OK);
            var curl = libcurl.easyInit();
            code = libcurl.easySetOpt(curl, CURLoption.CURLOPT_URL, "https://example.com");
            assertEquals(code, CURLcode.CURLE_OK);
            code = libcurl.easySetOpt(curl, CURLoption.CURLOPT_FOLLOWLOCATION, 1L);
            assertEquals(code, CURLcode.CURLE_OK);

            code = libcurl.easyPerform(curl);
            assertEquals(code, CURLcode.CURLE_OK);
        }
    }
```
8) Run the main method. You should see the html page of `https://example.com` in stdout.

## Example usages
### Example 1
Generate all structures, enums and unions from `gpio.h` and generate file `GPIONative.java` with call of native function `ioctl` with errno support.
```java
@NativeMemory(header = "gpio.h")
@Structs
@Enums
@Unions
public interface GPIO {
    @NativeManualFunction(name = "ioctl", useErrno = true)
    int nativeCall(int fd, long command, int data) throws NativeMemoryException;
}
```
### Example 2
Generate only structures with specified name mapping from kernel header and generate file `GPIONative.java` with call of native function `ioctl` without errno support.
Method declaration will forward the output of `ioctl` to user code, since `returnType` option and return type of method are the same  
```java
@NativeMemory(header = "/usr/src/linux-headers-6.2.0-39/include/uapi/linux/gpio.h")
@Structs({
        @Struct(name = "gpiochip_info", javaName = "ChipInfo"),
        @Struct(name = "gpio_v2_line_info", javaName = "LineInfo")
})
public interface GPIO {
    @NativeManualFunction(name = "ioctl")
    int nativeCall(int fd, long command, int data) throws NativeMemoryException;
}
```

### Example 3

Generate only structures with specified name mapping from kernel header and generate file `GPIONative.java` with call of two native functions `ioctl`.
You can declare a freshly generated structure to use within the native functions. The difference between methods are in native function declaration:
- first one declare a native function `int ioctl(int, long, gpiochip_info*)`, which  passes `data` as pointer, writes the data to structure and returns it to user code as generated object
- the second one returns the `data` variable with type long as a pointer and returns it to user code

```java
@NativeMemory(header = "/usr/src/linux-headers-6.2.0-39/include/uapi/linux/gpio.h")
@Structs({
        @Struct(name = "gpiochip_info", javaName = "ChipInfo"),
        @Struct(name = "gpio_v2_line_info", javaName = "LineInfo")
})
public interface GPIO {
    @NativeManualFunction(name = "ioctl")
    ChipInfo nativeCall(int fd, long command, @Returns ChipInfo data) throws NativeMemoryException;

    @NativeManualFunction(name = "ioctl", useErrno = true)
    long nativeCall(int fd, long command, @Returns @ByAddress long data) throws NativeMemoryException;
}
```
### Example 4

You can use system properties `-Dversion=6.2.0-39` to pass variables into header path definition and define options:
- add paths for `libclang` include lookup.
- specify generated code location with `packageName`
- combine all top level `#define` in header file to `GPIOConstant` enum  (if all source variables are of the same type) or static class (if source variable are of different types)

```java
@NativeMemory(header = "/usr/src/linux-headers-${version}/include/uapi/linux/gpio.h")
@NativeMemoryOptions(
        includes = "/usr/lib/llvm-15/lib/clang/15.0.7/include/",
        packageName = "org.my.project",
        processRootConstants = true
)
@Structs
public interface GPIO {
    @NativeManualFunction(name = "ioctl")
    GpiochipInfo nativeCall(int fd, long command, @Returns GpiochipInfo data) throws NativeMemoryException;
}
```

### Example 5
Generate `some_struct` structure from `library2_header.h` and implement two native functions:
- `some_func1` from library `library1` with provided absolute path
- `some_func2` from library `library2` with just name. Java will try to load the library using standard POSIX pattern (e.g. on Linux will look over `LD_LIBRARY_PATH`)
```java
@NativeMemory(header = "library2_header.h")
@Structs
public interface GPIO {
    @NativeManualFunction(name = "some_func1", library = "/some/path/to/library1.so")
    void call(int data) throws NativeMemoryException;

    @NativeManualFunction(name = "some_func2", library = "library2")
    SomeStruct nativeCall(int param, @Returns SomeStruct data) throws NativeMemoryException;
}
```
package io.github.digitalsmile.gpio.functions;

import io.github.digitalsmile.annotation.NativeMemoryException;
import org.junit.jupiter.api.Test;

public class LibcFunctionTests {

    @Test
    public void testOpenRead() throws NativeMemoryException {
        var file = new LibcFunctionsNative();
        var osRelease = file.open("/etc/os-release", 0);
        var buffer = file.read(osRelease, new byte[1024], 1024);
        System.out.println(new String(buffer));
        file.close();
    }
}

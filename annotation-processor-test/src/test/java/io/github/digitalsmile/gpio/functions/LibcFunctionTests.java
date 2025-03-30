package io.github.digitalsmile.gpio.functions;

import io.github.digitalsmile.annotation.NativeMemoryException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
public class LibcFunctionTests {

    @Test
    public void testOpenRead() throws NativeMemoryException {
        var file = new LibcFunctionsNative();
        var osRelease = file.open("/etc/os-release", 0);
        var buffer = new String(file.read(osRelease, new byte[1024], 1024));
        assertTrue(buffer.contains("Ubuntu"));
        file.close();
    }
}

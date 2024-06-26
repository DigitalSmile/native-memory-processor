package io.github.digitalsmile;

import io.github.digitalsmile.annotation.function.NativeMemoryException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
public class SimpleTest {

    @Test
    public void testPid() throws NativeMemoryException {
        var pid = new PidNative().nativeCall();
        assertTrue(pid > 0, "Pid should be greater than 0. Got: " + pid);
    }
}

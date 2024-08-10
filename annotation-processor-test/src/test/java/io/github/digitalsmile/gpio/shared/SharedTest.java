package io.github.digitalsmile.gpio.shared;


import io.github.digitalsmile.gpio.shared.system.Stat;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SharedTest {

    @Test
    public void shared() throws Throwable {
        var file = Path.of(Objects.requireNonNull(SharedTest.class.getResource("/shared/test1.h")).toURI()).toFile().getAbsolutePath();
        int fd;
        try (var fileLib = new SharedTestOneNative()) {
            fd = fileLib.open(file, 0);
        }

        try (var statLib = new SharedTestTwoNative()) {
            var stats = statLib.stat(fd, Stat.createEmpty());
            assertEquals(stats.stSize(), 35);
        }
    }
}

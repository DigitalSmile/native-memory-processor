package io.github.digitalsmile.library;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;

public class LibraryPath {
    public static String extractAndGetPath(String libraryName) {
        libraryName += getLibraryExtension();

        try (var in = LibraryPath.class.getClassLoader().getResourceAsStream("lib/" + libraryName)) {
            if (in == null) {
                throw new IOException("Cannot open path with libraries");
            }
            var path = Files.createTempFile(libraryName + "-", "");
            if (!path.toFile().exists()) {
                throw new IOException("Temp file " + path + "does not exists!");
            }
            var file = path.toFile();
            file.deleteOnExit();
            try (var out = new FileOutputStream(file)) {
                int count;
                byte[] buf = new byte[16 * 1024];
                while ((count = in.read(buf)) > 0) {
                    out.write(buf, 0, count);
                }
            }
            return file.getAbsolutePath();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String getLibraryExtension() {
        var osType = OsCheck.getOperatingSystemType();
        switch (osType) {
            case Linux -> {
                return ".so";
            }
            case MacOS -> {
                return ".dylib";
            }
            default -> throw new IllegalArgumentException("Unsupported OS " + osType);
        }
    }
}

package io.github.digitalsmile.library;

import java.util.Locale;

/**
 * helper class to check the operating system this Java VM runs in
 * <p>
 * please keep the notes below as a pseudo-license
 * <p>
 * <a href="http://stackoverflow.com/questions/228477/how-do-i-programmatically-determine-operating-system-in-java">Soultion</a>
 * compare to <a href="http://svn.terracotta.org/svn/tc/dso/tags/2.6.4/code/base/common/src/com/tc/util/runtime/Os.java">other</a>
 * <a href="http://www.docjar.com/html/api/org/apache/commons/lang/SystemUtils.java.html">ones</a>
 */
public final class OsCheck {
    /**
     * Types of Operating Systems
     */
    public enum OSType {
        Windows, MacOS, Linux, Other
    }

    ;

    // cached result of OS detection
    private static OSType detectedOS;

    /**
     * Detect the operating system from the os.name System property and cache
     * the result.
     *
     * @return - the operating system detected
     */
    public static OSType getOperatingSystemType() {
        if (detectedOS == null) {
            var OS = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
            if ((OS.contains("mac")) || (OS.contains("darwin"))) {
                detectedOS = OSType.MacOS;
            } else if (OS.contains("win")) {
                detectedOS = OSType.Windows;
            } else if (OS.contains("nux")) {
                detectedOS = OSType.Linux;
            } else {
                detectedOS = OSType.Other;
            }
        }
        return detectedOS;
    }
}

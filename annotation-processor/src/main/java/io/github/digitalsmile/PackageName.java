package io.github.digitalsmile;

import java.util.HashMap;
import java.util.Map;

public class PackageName {

    private static String DEFAULT_PACKAGE_NAME = "";
    private static final Map<String, String> packageNamesMap = new HashMap<>();

    public static void addPackage(String typeName, String packageName) {
        packageNamesMap.put(typeName, DEFAULT_PACKAGE_NAME + "." + packageName);
    }

    public static void addPackage(String typeName) {
        packageNamesMap.put(typeName, DEFAULT_PACKAGE_NAME);
    }

    public static String getPackageName(String typeName) {
        return packageNamesMap.getOrDefault(typeName, DEFAULT_PACKAGE_NAME);
    }

    public static void setDefaultPackageName(String packageName) {
        DEFAULT_PACKAGE_NAME = packageName;
    }
}

package io.github.digitalsmile;

public enum ParsingOption {
    HEADER_FILE("headerFile"),
    PACKAGE_NAME("packageName"),
    ROOT_ENUM_CREATION("createEnumFromRootDefines");

    private final String option;
    ParsingOption(String option) {
        this.option = option;
    }

    public String getOption() {
        return option;
    }
}

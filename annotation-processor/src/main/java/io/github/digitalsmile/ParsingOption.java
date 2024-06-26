package io.github.digitalsmile;

public enum ParsingOption {
    HEADER_FILE("headerFile"),
    PACKAGE_NAME("packageName"),
    ROOT_ENUM_CREATION("createEnumFromRootDefines"),
    ALL_PARSING_LIST("allParsingList");

    private final String option;
    ParsingOption(String option) {
        this.option = option;
    }

    public String getOption() {
        return option;
    }
}

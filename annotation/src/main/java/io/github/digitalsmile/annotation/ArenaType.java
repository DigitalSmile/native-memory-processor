package io.github.digitalsmile.annotation;

/**
 * Class represents <code>Arena</code> type in string representation for code generation.
 */
public enum ArenaType {
    /**
     * Arena.global()
     */
    GLOBAL,
    /**
     * Arena.ofAuto()
     */
    AUTO,
    /**
     * Arena.ofConfined()
     */
    CONFINED,
    /**
     * Arena.ofShared()
     */
    SHARED;

    /**
     * Gets the string representation of arena type.
     *
     * @return string representation of arena type
     */
    public String arena() {
        return switch (this) {
            case AUTO -> "ofAuto()";
            case GLOBAL -> "global()";
            case CONFINED -> "ofConfined()";
            case SHARED -> "ofShared()";
        };
    }
}

package io.github.digitalsmile.annotation;

import java.lang.foreign.Arena;

public enum ArenaType {
    GLOBAL, AUTO, CONFINED, SHARED;

    public String arena() {
        return switch (this) {
            case AUTO -> "ofAuto()";
            case GLOBAL -> "global()";
            case CONFINED -> "ofConfined()";
            case SHARED -> "ofShared()";
        };
    }
}

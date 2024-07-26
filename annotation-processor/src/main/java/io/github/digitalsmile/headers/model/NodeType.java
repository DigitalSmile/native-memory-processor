package io.github.digitalsmile.headers.model;

public enum NodeType {
    ROOT, OPAQUE,
    STRUCT, ENUM, UNION,
    ANON_STRUCT, ANON_ENUM, ANON_UNION,
    VARIABLE;

    public boolean isAnonymous() {
        return this.equals(ANON_STRUCT) || this.equals(ANON_ENUM) || this.equals(ANON_UNION);
    }

    public boolean isVariable() {
        return this.equals(VARIABLE);
    }
}

package io.github.digitalsmile.headers.model;

public enum NodeType {
    ROOT, OPAQUE,
    STRUCT, ENUM, UNION,
    ANON_STRUCT, ANON_ENUM, ANON_UNION, POINTER,
    VARIABLE,
    FUNCTION;

    public boolean isAnonymous() {
        return this.equals(ANON_STRUCT) || this.equals(ANON_ENUM) || this.equals(ANON_UNION);
    }

    public boolean isVariable() {
        return this.equals(VARIABLE);
    }

    public boolean isPointer() {
        return this.equals(POINTER);
    }

    public boolean isOpaque() {
        return this.equals(OPAQUE);
    }

    public boolean isEnum() {
        return this.equals(ENUM);
    }

    public boolean isFunction() {
        return this.equals(FUNCTION);
    }
}

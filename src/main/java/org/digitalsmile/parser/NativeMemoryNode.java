package org.digitalsmile.parser;

import org.openjdk.jextract.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class NativeMemoryNode {
    private final String name;
    private final Type type;
    private final Object value;
    private final long arraySize;
    private final List<NativeMemoryNode> nodes = new ArrayList<>();

    public NativeMemoryNode(String name, Type type, long arraySize) {
        this.name = name;
        this.type = type;
        this.value = null;
        this.arraySize = arraySize;
    }

    public NativeMemoryNode(String name, Type type) {
        this.name = name;
        this.type = type;
        this.value = null;
        this.arraySize = 0;
    }

    public NativeMemoryNode(String name, Type type, Object value) {
        this.name = name;
        this.type = type;
        this.value = value;
        this.arraySize = 0;
    }

    public void addNode(NativeMemoryNode child) {
        nodes.add(child);
    }

    public void addNode(String name, Type type) {
        nodes.add(new NativeMemoryNode(name, type));
    }

    public void addArrayNode(String name, Type type, long arraySize) {
        nodes.add(new NativeMemoryNode(name, type, arraySize));
    }

    public String getName() {
        return name;
    }

    public String getPrettyName() {
        return PrettyName.get(name);
    }

    public Type getType() {
        return type;
    }

    public Object getValue() {
        return value;
    }

    public List<NativeMemoryNode> getNodes() {
        return nodes;
    }

    public boolean isArray() {
        return arraySize > 0;
    }

    public long getArraySize() {
        return arraySize;
    }

    @Override
    public String toString() {
        return "NativeMemoryNode{" +
                "name='" + name + '\'' +
                ", type=" + type +
                ", arraySize=" + arraySize +
                ", nodes=" + nodes +
                '}';
    }
}

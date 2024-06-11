package org.digitalsmile.parser;

import org.openjdk.jextract.Type;

import java.util.ArrayList;
import java.util.List;

public class NativeMemoryNode {
    private final String name;
    private final Type type;
    private final long arraySize;
    private final List<NativeMemoryNode> nodes = new ArrayList<>();

    public NativeMemoryNode(String name, Type type, long arraySize) {
        this.name = name;
        this.type = type;
        this.arraySize = arraySize;
    }

    public NativeMemoryNode(String name, Type type) {
        this.name = name;
        this.type = type;
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
        if (name.matches("([a-z]+[a-zA-Z0-9]+)+")) {
            return name;
        }
        var words = name.split("[\\W_-]+");
        var builder = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            var word = words[i];
            if (i == 0) {
                word = word.isEmpty() ? word : word.toLowerCase();
            } else {
                word = word.isEmpty() ? word : Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase();
            }
            builder.append(word);
        }
        return builder.toString();
    }

    public Type getType() {
        return type;
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

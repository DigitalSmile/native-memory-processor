package org.digitalsmile.parser;

import org.openjdk.jextract.Type;

import java.util.ArrayList;
import java.util.List;

public class NativeMemoryModel {
    private final String structName;
    private final List<NativeMemoryNode> nodes = new ArrayList<>();

    public NativeMemoryModel(String structName) {
        this.structName = structName;
    }

    public void addNode(NativeMemoryNode node) {
        nodes.add(node);
    }

    public void addNode(String name, Type type) {
        nodes.add(new NativeMemoryNode(name, type));
    }
    public void addArrayNode(String name, Type type, long arraySize) {
        nodes.add(new NativeMemoryNode(name, type, arraySize));
    }

    public List<NativeMemoryNode> getNodes() {
        return nodes;
    }

    public String getStructName() {
        return structName;
    }

    @Override
    public String toString() {
        return "NativeMemoryModel{" +
                "structName='" + structName + '\'' +
                ", nodes=" + nodes +
                '}';
    }
}

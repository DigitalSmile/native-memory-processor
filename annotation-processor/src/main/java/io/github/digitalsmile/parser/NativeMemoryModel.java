package io.github.digitalsmile.parser;

import org.openjdk.jextract.Type;

import java.util.ArrayList;
import java.util.List;

public class NativeMemoryModel {
    private final String fileName;
    private final List<NativeMemoryNode> nodes = new ArrayList<>();

    public NativeMemoryModel(String fileName) {
        this.fileName = fileName;
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

    public String getFileName() {
        return fileName;
    }

    @Override
    public String toString() {
        return "NativeMemoryModel{" +
                "fileName='" + fileName + '\'' +
                ", nodes=" + nodes +
                '}';
    }
}

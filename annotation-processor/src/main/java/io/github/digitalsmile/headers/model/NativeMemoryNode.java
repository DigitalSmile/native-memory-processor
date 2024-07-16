package io.github.digitalsmile.headers.model;

import io.github.digitalsmile.headers.mapping.NodeType;
import io.github.digitalsmile.headers.mapping.OriginalType;

import java.util.ArrayList;
import java.util.List;

public class NativeMemoryNode {
    private final String name;
    private final List<NativeMemoryNode> nodes = new ArrayList<>();
    private final NodeType nodeType;
    private final OriginalType type;
    private final Object value;
    private final String source;

    public NativeMemoryNode(String name, NodeType nodeType, OriginalType type, String source, Object value) {
        this.name = name;
        this.nodeType = nodeType;
        this.type = type;
        this.source = source;
        this.value = value;
    }

    public NativeMemoryNode(String name, NodeType nodeType, OriginalType type, String source) {
        this(name, nodeType, type, source, null);
    }

    public String getName() {
        return name;
    }

    public NodeType getNodeType() {
        return nodeType;
    }

    public OriginalType getType() {
        return type;
    }

    public Object getValue() {
        return value;
    }

    public String getSource() {
        return source;
    }

    public void addNode(NativeMemoryNode node) {
        this.nodes.add(node);
    }

    public void addNodes(List<NativeMemoryNode> nodes) {
        this.nodes.addAll(nodes);
    }

    public List<NativeMemoryNode> nodes() {
        return nodes;
    }

    @Override
    public String toString() {
        return name + " (" + (nodeType.equals(NodeType.VARIABLE) ? type : nodeType) + ")" + (!nodes.isEmpty() ? ": " + nodes.size() + " " + nodes : "");
    }
}

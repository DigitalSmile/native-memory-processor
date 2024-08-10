package io.github.digitalsmile.headers.model;

import io.github.digitalsmile.headers.mapping.OriginalType;
import org.barfuin.texttree.api.Node;
import org.openjdk.jextract.Position;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class NativeMemoryNode implements Node {
    private final String name;
    private final List<NativeMemoryNode> nodes = new ArrayList<>();
    private final NodeType nodeType;
    private final OriginalType type;
    private final Object value;
    private final int level;
    private final Position position;

    public NativeMemoryNode(String name, NodeType nodeType, OriginalType type, int level, Position position, Object value) {
        this.name = name;
        this.nodeType = nodeType;
        this.type = type;
        this.level = level;
        this.position = position;
        this.value = value;
    }

    public NativeMemoryNode(String name, OriginalType type, int level, Position position, Object value) {
        this(name, NodeType.VARIABLE, type, level, position, value);
    }

    public NativeMemoryNode(String name, OriginalType type, int level, Position position) {
        this(name, NodeType.VARIABLE, type, level, position, null);
    }

    public NativeMemoryNode(String name, NodeType nodeType, OriginalType type, int level, Position position) {
        this(name, nodeType, type, level, position, null);
    }

    public NativeMemoryNode(String name, NodeType nodeType) {
        this(name, nodeType, null, 0, Position.NO_POSITION, null);
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

    public int getLevel() {
        return level;
    }

    public Position getPosition() {
        return position;
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

    @Override
    public String getText() {
        var builder = new StringBuilder();
        builder.append(name);
        if (nodeType.isFunction()) {
            builder.append(" ").append(nodeType).append(": ").append(type).append(" ").append(position);
        } else if (!nodeType.equals(NodeType.VARIABLE) && !nodeType.equals(NodeType.ROOT) && level <= 1) {
            builder.append(" ").append(nodeType).append(" from ").append(position);
        } else if (!nodeType.equals(NodeType.ROOT)) {
            builder.append(" ");
            if (!nodeType.equals(NodeType.VARIABLE)) {
                builder.append(nodeType);
                if (!nodeType.isAnonymous()) {
                    builder.append(" of type ").append(type);
                }
            } else {
                builder.append("(").append(type).append(")");
                if (value != null) {
                    builder.append(" ").append(value);
                }
            }
        }

        return builder.toString();
    }

    @Override
    public Iterable<? extends Node> getChildren() {
        return nodes;
    }
}

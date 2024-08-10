package io.github.digitalsmile.headers;

import io.github.digitalsmile.PackageName;
import io.github.digitalsmile.headers.model.NativeMemoryNode;
import io.github.digitalsmile.headers.model.NodeType;
import org.barfuin.texttree.api.TextTree;
import org.barfuin.texttree.api.TreeOptions;
import org.barfuin.texttree.api.style.TreeStyles;
import org.openjdk.jextract.Declaration;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class Parser {

    private final String packageName;
    private final Messager messager;
    private final Filer filer;

    public Parser(String packageName, Messager messager, Filer filer) {
        this.packageName = packageName;
        this.messager = messager;
        this.filer = filer;
    }

    public List<NativeMemoryNode> parse(List<String> structs, List<String> enums,
                                        List<String> unions,
                                        Map<Path, Declaration.Scoped> parsed, boolean debug, boolean systemHeader) {

        List<NativeMemoryNode> nodes = new ArrayList<>();
        for (Map.Entry<Path, Declaration.Scoped> entry : parsed.entrySet()) {
            var rootName = entry.getKey().toFile().getAbsolutePath();//.getName().split("\\.")[0];
            var declarationParser = new DeclarationParser(messager, structs, enums, unions, systemHeader);
            var root = new NativeMemoryNode(rootName, NodeType.ROOT);
            declarationParser.parseRoot(entry.getValue(), root);
            if (debug) {
                createDebugFile(root, "raw.txt");
            }

            var newRoot = new NativeMemoryNode(rootName, NodeType.ROOT);
            List<NativeMemoryNode> systemHeaders = Collections.emptyList();
            if (systemHeader) {
                systemHeaders = flatten(root.nodes()).stream().filter(p -> p.getPosition().isSystemHeader()).toList();
                clearFromSystemHeaders(root, systemHeaders);
            }
            prepareStructure(newRoot, root, systemHeaders, systemHeader);

            newRoot.addNodes(root.nodes());

            if (debug) {
                createDebugFile(newRoot, "structured.txt");
            }

            nodes.add(newRoot);
        }
        return nodes;
    }

    private List<NativeMemoryNode> flatten(List<NativeMemoryNode> nodes) {
        return nodes.stream().flatMap(i -> Stream.concat(Stream.of(i), flatten(i.nodes()).stream())).toList();
    }

    private void clearFromSystemHeaders(NativeMemoryNode parentNode, List<NativeMemoryNode> systemHeaderNodes) {
        var iterator = parentNode.nodes().listIterator();

        while (iterator.hasNext()) {
            var node = iterator.next();
            if (systemHeaderNodes.stream().anyMatch(p -> p.getPosition().equals(node.getPosition()))) {
                iterator.remove();
                continue;
            }
            clearFromSystemHeaders(node, systemHeaderNodes);
        }
    }

    private void prepareStructure(NativeMemoryNode rootNode, NativeMemoryNode parentNode, List<NativeMemoryNode> systemHeaderNodes, boolean systemHeader) {
        var iterator = parentNode.nodes().listIterator();

        while (iterator.hasNext()) {
            var node = iterator.next();
            if (node.getLevel() == 0) {
                prepareStructure(rootNode, node, systemHeaderNodes, systemHeader);
            } else {
                var nodeType = node.getNodeType();
                var type = node.getType();

            if (systemHeader) {
                var foundReference = systemHeaderNodes.stream().filter(p -> p.getName().equals(type.typeName())).findFirst().orElse(null);
                if (foundReference != null && !rootNode.nodes().contains(foundReference)) {
                    rootNode.addNode(foundReference);
                    prepareStructure(rootNode, foundReference, systemHeaderNodes, systemHeader);
                }
            }

                var contains = rootNode.nodes().stream().anyMatch(p -> p.getName().equals(type.typeName()));
                if (node.getLevel() > 1 && !nodeType.isVariable() && !nodeType.equals(NodeType.ANON_UNION) && !node.nodes().isEmpty() && !contains) {
                    var newNodeType = makeNotAnonymous(nodeType);
                    var newNode = new NativeMemoryNode(node.getName(), newNodeType, node.getType(), 1, node.getPosition(), node.getValue());
                    iterator.set(newNode);
                    var typeName = type.typeName();
                    PackageName.addPackage(typeName, "nested");
                    var rebuildNode = new NativeMemoryNode(typeName, newNodeType, node.getType(), node.getLevel(), node.getPosition(), node.getValue());
                    rebuildNode.addNodes(node.nodes());
                    rootNode.addNode(rebuildNode);
                }
                prepareStructure(rootNode, node, systemHeaderNodes, systemHeader);
            }
        }
    }

    private NodeType makeNotAnonymous(NodeType type) {
        if (type.equals(NodeType.ANON_UNION)) {
            return NodeType.UNION;
        } else if (type.equals(NodeType.ANON_ENUM)) {
            return NodeType.ENUM;
        } else if (type.equals(NodeType.ANON_STRUCT)) {
            return NodeType.STRUCT;
        }
        return type;
    }

    private void createDebugFile(NativeMemoryNode rootNode, String fileName) {
        try {
            var generatedFile = filer.createResource(StandardLocation.SOURCE_OUTPUT, packageName, fileName, (Element[]) null);
            var writer = generatedFile.openWriter();
            var treeOptions = new TreeOptions();
            treeOptions.setStyle(TreeStyles.UNICODE);
            writer.write(TextTree.newInstance(treeOptions).render(rootNode));
            writer.close();
        } catch (IOException e) {
            messager.printMessage(Diagnostic.Kind.ERROR, "Exception occurred while processing file '" + fileName + "': " + e.getMessage());
        }
    }
}

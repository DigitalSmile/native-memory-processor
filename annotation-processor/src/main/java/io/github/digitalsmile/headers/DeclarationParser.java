package io.github.digitalsmile.headers;

import io.github.digitalsmile.PackageName;
import io.github.digitalsmile.headers.mapping.OriginalType;
import io.github.digitalsmile.headers.model.NativeMemoryNode;
import io.github.digitalsmile.headers.model.NodeType;
import org.openjdk.jextract.Declaration;
import org.openjdk.jextract.Type;

import javax.annotation.processing.Messager;
import javax.tools.Diagnostic;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class DeclarationParser {
    private final List<String> structs;
    private final List<String> unions;
    private final List<String> enums;
    private final Messager messager;

    private final boolean parseSystemHeaders;

    private final boolean parseNoStructs;
    private final boolean parseNoEnums;
    private final boolean parseNoUnions;

    public DeclarationParser(Messager messager, List<String> structs, List<String> enums, List<String> unions,
                             boolean parseSystemHeaders) {
        this.messager = messager;
        this.parseSystemHeaders = parseSystemHeaders;

        this.parseNoStructs = structs == null;
        this.parseNoEnums = enums == null;
        this.parseNoUnions = unions == null;

        this.structs = structs != null ? structs : Collections.emptyList();
        this.unions = unions != null ? unions : Collections.emptyList();
        this.enums = enums != null ? enums : Collections.emptyList();
    }

    private boolean notInScope(Declaration declaration) {
        return declaration.pos().isSystemHeader();
    }

    private int structCount = 0;
    private int enumCount = 0;
    private int unionCount = 0;

    public void parseRoot(Declaration.Scoped root, NativeMemoryNode rootNode) {
        parseDeclarations(root, rootNode, true);
    }

    public void parseDeclarations(Declaration.Scoped parent, NativeMemoryNode parentNode, boolean skipSystemHeaders) {
        for (Declaration declaration : parent.members()) {
            if (skipSystemHeaders && notInScope(declaration) && !parseSystemHeaders) {
                continue;
            }
            parseDeclaration(declaration, parentNode, parent.kind().equals(Declaration.Scoped.Kind.TOPLEVEL));
        }
    }

    private boolean skipParsing(String declarationName, Declaration.Scoped.Kind kind) {
        var contains = Stream.of(structs, enums, unions).flatMap(List::stream).filter(t -> t.equals(declarationName)).findFirst().orElse(null) != null;
        return switch (kind) {
            case STRUCT -> parseNoStructs || (!contains && !structs.isEmpty());
            case ENUM -> parseNoEnums || (!contains && !enums.isEmpty());
            case UNION -> parseNoUnions || (!contains && !unions.isEmpty());
            default -> true;
        };
    }

    private String parseScoped(Declaration.Scoped declarationScoped, NodeType nodeType, String namePrefix) {
        var name = declarationScoped.name();
        switch (declarationScoped.kind()) {
            case STRUCT -> {
                if (nodeType.isAnonymous()) {
                    name = namePrefix + "_struct_" + structCount++;
                }
                PackageName.addPackage(name, nodeType.equals(NodeType.OPAQUE) ? "opaque" : "structs");
            }
            case ENUM -> {
                if (nodeType.isAnonymous()) {
                    name = namePrefix + "_enum_" + enumCount++;
                }
                PackageName.addPackage(name, "enums");
            }
            case UNION -> {
                if (nodeType.isAnonymous()) {
                    name = namePrefix + "_union_" + unionCount++;
                }
                PackageName.addPackage(name, "unions");
            }
            case BITFIELDS -> {
                messager.printMessage(Diagnostic.Kind.WARNING, "unsupported scoped kind " + declarationScoped.kind() + " " + declarationScoped.pos());
                return name;
            }
        }
        return name;
    }

    public void parseDeclaration(Declaration declaration, NativeMemoryNode parentNode, boolean isTopLevel) {
        switch (declaration) {
            case Declaration.Scoped declarationScoped -> {
                if (skipParsing(declaration.name(), declarationScoped.kind())) {
                    return;
                }
                var nodeType = getNodeType(declarationScoped, declarationScoped.attributes());
                var namePrefix = isTopLevel ? parentNode.getName() : "internal";
                var name = parseScoped(declarationScoped, nodeType, namePrefix);
                var node = new NativeMemoryNode(name, nodeType, OriginalType.ofObject(name), parentNode.getLevel() + 1, declarationScoped.pos());
                parseRoot(declarationScoped, node);
                parentNode.addNode(node);
            }
            case Declaration.Constant declarationConstant -> {
                var node = parseVariable(declarationConstant, declarationConstant.type(), parentNode.getLevel());
                if (node == null) {
                    return;
                }
                if (isTopLevel) {
                    var constantsEnum = parentNode.nodes().stream().filter(p -> p.getName().equals(parentNode.getName() + "_constants")).findFirst();
                    if (constantsEnum.isPresent()) {
                        constantsEnum.get().addNode(node);
                    } else {
                        var enumNode = new NativeMemoryNode(parentNode.getName() + "_constants", NodeType.ENUM);
                        enumNode.addNode(node);
                        parentNode.addNode(enumNode);
                    }
                } else {
                    parentNode.addNode(node);
                }
            }
            case Declaration.Bitfield declarationBitfield ->
                    messager.printMessage(Diagnostic.Kind.WARNING, "unsupported declaration type " + declarationBitfield.type());
            case Declaration.Variable declarationVariable -> {
                switch (declarationVariable.kind()) {
                    case GLOBAL, BITFIELD, PARAMETER ->
                            messager.printMessage(Diagnostic.Kind.WARNING, "unsupported declaration kind " + declarationVariable.kind() + " " + declarationVariable.pos());
                    case FIELD -> {
                        var node = parseVariable(declarationVariable, declarationVariable.type(), parentNode.getLevel());
                        if (node != null) {
                            parentNode.addNode(node);
                        }
                    }
                }
            }
            case Declaration.Typedef declarationTypedef -> {
                var node = parseVariable(declarationTypedef, declarationTypedef.type(), parentNode.getLevel());
                if (node != null) {
                    parentNode.addNode(node);
                }
            }
            case Declaration.Function _ -> {
            }
            default ->
                    messager.printMessage(Diagnostic.Kind.WARNING, "unsupported declaration type " + declaration.name() + " " + declaration.pos());

        }
    }


    private NativeMemoryNode parseVariable(Declaration declaration, Type type, int level) {
        if (type.isErroneous()) {
            printWarning(declaration, "type " + type + " is not valid C/C++ constructions and will be skipped.");
            return null;
        }
        var nextLevel = level + 1;
        var source = declaration.pos();
        switch (type) {
            case Type.Array typeArray -> {
                if (typeArray.elementType().isErroneous()) {
                    printWarning(declaration, "type " + type + " is not valid C/C++ constructions and will be skipped.");
                    return null;
                }
                return new NativeMemoryNode(declaration.name(), OriginalType.of(typeArray), nextLevel, source);
            }
            case Type.Primitive typePrimitive -> {
                if (declaration instanceof Declaration.Constant declarationConstant) {
                    return new NativeMemoryNode(declaration.name(), OriginalType.of(typePrimitive), nextLevel, source, declarationConstant.value());
                } else {
                    if (typePrimitive.kind().equals(Type.Primitive.Kind.Void)) {
                        PackageName.addPackage(declaration.name(), "opaque");
                        return new NativeMemoryNode(declaration.name(), NodeType.OPAQUE, OriginalType.ofObject(declaration.name()), nextLevel, source);
                    } else {
                        return new NativeMemoryNode(declaration.name(), OriginalType.of(typePrimitive), nextLevel, source);
                    }
                }
            }
            case Type.Delegated typeDelegated -> {
                switch (typeDelegated.kind()) {
                    case SIGNED, UNSIGNED -> {
                        if (declaration instanceof Declaration.Constant declarationConstant) {
                            return new NativeMemoryNode(declaration.name(), OriginalType.of(typeDelegated), nextLevel, source, declarationConstant.value());
                        } else {
                            return new NativeMemoryNode(declaration.name(), OriginalType.of(typeDelegated), nextLevel, source);
                        }
                    }
                    case POINTER, TYPEDEF -> {
                        var originalType = OriginalType.of(typeDelegated.type());
                        if (originalType.carrierClass().equals(byte.class)) {
                            originalType = OriginalType.ofObject(String.class.getSimpleName());
                        }
                        if (declaration instanceof Declaration.Constant declarationConstant) {
                            return new NativeMemoryNode(declaration.name(), originalType, nextLevel, source, declarationConstant.value());
                        } else {
                            return new NativeMemoryNode(declaration.name(), NodeType.POINTER, originalType, nextLevel, source);
                        }
                    }
                    default -> printWarning(declaration, "unsupported variable kind " + typeDelegated.kind());
                }
            }
            case Type.Declared typeDeclared -> {
                var typeName = typeDeclared.tree().name();
                var nodeType = getNodeType(typeDeclared.tree(), declaration.attributes());
                if (nodeType.isAnonymous()) {
                    typeName = "_" + declaration.name();
                }
                var node = new NativeMemoryNode(declaration.name(), nodeType, OriginalType.ofObject(typeName), nextLevel, source);
                if (nodeType.isAnonymous()) {
                    parseRoot(typeDeclared.tree(), node);
                    return node;
                } else if (notInScope(typeDeclared.tree())) {
                    parseDeclarations(typeDeclared.tree(), node, false);
                    return node;
                } else if (level >= 1) {
                    return node;
                } else {
                    return null;
                }
            }
            default -> printWarning(declaration, "unknown field type " + type);
        }
        return null;
    }

    private NodeType getNodeType(Declaration.Scoped declaration, Collection<Record> attributes) {
        if (declaration.members().isEmpty()) {
            return NodeType.OPAQUE;
        }
        var it = attributes.iterator();
        var isAnonymous = false;
        while (it.hasNext()) {
            var name = it.next().getClass().getSimpleName();
            if (name.equals("AnonymousStruct") || declaration.name().contains("(unnamed at")) {
                isAnonymous = true;
            }
        }
        switch (declaration.kind()) {
            case STRUCT -> {
                if (isAnonymous) {
                    return NodeType.ANON_STRUCT;
                } else {
                    return NodeType.STRUCT;
                }
            }
            case ENUM -> {
                if (isAnonymous) {
                    return NodeType.ANON_ENUM;
                } else {
                    return NodeType.ENUM;
                }
            }
            case UNION -> {
                if (isAnonymous) {
                    return NodeType.ANON_UNION;
                } else {
                    return NodeType.UNION;
                }
            }
            default -> {
                return NodeType.VARIABLE;
            }
        }
    }

    private void printError(Declaration declaration, String message) {
        messager.printMessage(Diagnostic.Kind.ERROR, declaration.pos() + " " + declaration.name() + ": " + message);
    }

    private void printWarning(Declaration declaration, String message) {
        messager.printMessage(Diagnostic.Kind.WARNING, declaration.pos() + ": " + message);
    }


}

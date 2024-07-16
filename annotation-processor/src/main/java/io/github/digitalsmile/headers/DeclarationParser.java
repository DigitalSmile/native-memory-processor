package io.github.digitalsmile.headers;

import io.github.digitalsmile.NativeProcessor;
import io.github.digitalsmile.PrettyName;
import io.github.digitalsmile.headers.model.NativeMemoryNode;
import io.github.digitalsmile.headers.mapping.NodeType;
import io.github.digitalsmile.headers.mapping.ObjectOriginalType;
import io.github.digitalsmile.headers.mapping.OriginalType;
import org.openjdk.jextract.Declaration;
import org.openjdk.jextract.Type;

import javax.annotation.processing.Messager;
import javax.tools.Diagnostic;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class DeclarationParser {
    private final Map<Declaration.Scoped.Kind, List<NativeMemoryNode>> model = new HashMap<>();
    private final Path headerPath;
    private final List<NativeProcessor.Type> structs;
    private final List<NativeProcessor.Type> unions;
    private final List<NativeProcessor.Type> enums;
    private final Messager messager;

    private final boolean parseNoStructs;
    private final boolean parseNoEnums;
    private final boolean parseNoUnions;

    public DeclarationParser(Messager messager, Path headerPath, List<NativeProcessor.Type> structs, List<NativeProcessor.Type> enums, List<NativeProcessor.Type> unions) {
        this.messager = messager;
        this.headerPath = headerPath;

        this.parseNoStructs = structs == null;
        this.parseNoEnums = enums == null;
        this.parseNoUnions = unions == null;

        this.structs = structs != null ? structs : Collections.emptyList();
        this.unions = unions != null ? unions : Collections.emptyList();
        this.enums = enums != null ? enums : Collections.emptyList();
    }

    private boolean declarationInFile(Declaration declaration) {
        return declaration.pos().path().equals(headerPath);
    }

    public Map<Declaration.Scoped.Kind, List<NativeMemoryNode>> getModel() {
        return model;
    }

    private int structCount = 0;
    private int enumCount = 0;
    private int unionCount = 0;

    public void parseDeclaration(Declaration.Scoped parent, NativeMemoryNode parentNode) {
        for (Declaration declaration : parent.members()) {
            if (!declarationInFile(declaration)) {
                continue;
            }
            var contains = Stream.of(structs, enums, unions).flatMap(List::stream).filter(t -> t.name().equals(declaration.name())).findFirst().orElse(null) != null;
            switch (declaration) {
                case Declaration.Scoped declarationScoped -> {
                    var nodeType = getNodeType(declarationScoped.kind(), declarationScoped.attributes());
                    switch (declarationScoped.kind()) {
                        case STRUCT -> {
                            if (parseNoStructs) {
                                continue;
                            }
                            if (!contains && !structs.isEmpty()) {
                                continue;
                            }
                            if (parent.kind().equals(Declaration.Scoped.Kind.TOPLEVEL)) {
                                var structNode = new NativeMemoryNode(declarationScoped.name(), nodeType, OriginalType.ofObject(declarationScoped.name()), declarationScoped.pos().toString());
                                parseDeclaration(declarationScoped, structNode);
                                model.computeIfAbsent(Declaration.Scoped.Kind.STRUCT, _ -> new ArrayList<>()).add(structNode);
                            } else {
                                var structNode = new NativeMemoryNode("struct_" + structCount++, nodeType, OriginalType.ofObject(declarationScoped.name()), declarationScoped.pos().toString());
                                parseDeclaration(declarationScoped, structNode);
                                parentNode.addNode(structNode);
                            }
                        }
                        case ENUM -> {
                            if (parseNoEnums) {
                                continue;
                            }
                            if (!contains && !enums.isEmpty()) {
                                continue;
                            }
                            if (parent.kind().equals(Declaration.Scoped.Kind.TOPLEVEL)) {
                                var name = declarationScoped.name();
                                OriginalType type;
                                if (name.startsWith("enum (unnamed")) {
                                    name = PrettyName.getObjectName(parentNode.getName()) + "Enum" + enumCount++;
                                    type = new ObjectOriginalType(name);
                                } else {
                                    type = OriginalType.ofObject(name);
                                }
                                var structNode = new NativeMemoryNode(name, nodeType, type, declarationScoped.pos().toString());
                                parseDeclaration(declarationScoped, structNode);
                                model.computeIfAbsent(Declaration.Scoped.Kind.ENUM, _ -> new ArrayList<>()).add(structNode);
                            } else {
                                var name = "enum_" + enumCount++;
                                OriginalType type;
                                if (nodeType.equals(NodeType.ANON_ENUM)) {
                                    type = new ObjectOriginalType(name);
                                } else {
                                    type = OriginalType.ofObject(declarationScoped.name());
                                }
                                var enumNode = new NativeMemoryNode(name, nodeType, type, declarationScoped.pos().toString());
                                parseDeclaration(declarationScoped, enumNode);
                                parentNode.addNode(enumNode);
                            }
                        }
                        case UNION -> {
                            if (parseNoUnions) {
                                continue;
                            }
                            if (!contains && !unions.isEmpty()) {
                                continue;
                            }
                            if (parent.kind().equals(Declaration.Scoped.Kind.TOPLEVEL)) {
                                OriginalType type;
                                var name = declarationScoped.name();
                                if (name.startsWith("union (unnamed")) {
                                    name = PrettyName.getObjectName(parentNode.getName()) + "Union" + enumCount++;
                                    type = new ObjectOriginalType(name);
                                } else {
                                    type = OriginalType.ofObject(name);
                                }
                                var structNode = new NativeMemoryNode(name, nodeType, type, declarationScoped.pos().toString());
                                parseDeclaration(declarationScoped, structNode);
                                model.computeIfAbsent(Declaration.Scoped.Kind.UNION, _ -> new ArrayList<>()).add(structNode);
                            } else {
                                var name = "union_" + unionCount++;
                                OriginalType type;
                                if (nodeType.equals(NodeType.ANON_UNION)) {
                                    type = new ObjectOriginalType(name);
                                } else {
                                    type = OriginalType.ofObject(declarationScoped.name());
                                }
                                var unionNode = new NativeMemoryNode(name, nodeType, type, declarationScoped.pos().toString());
                                parseDeclaration(declarationScoped, unionNode);
                                parentNode.addNode(unionNode);
                            }
                        }
                        default ->
                                messager.printMessage(Diagnostic.Kind.WARNING, "unsupported scoped kind " + declarationScoped.kind() + " " + declarationScoped.pos());
                    }
                }
                case Declaration.Constant declarationConstant ->
                        parentNode.addNode(parseVariable(declarationConstant, declarationConstant.type()));
                case Declaration.Bitfield declarationBitfield ->
                        messager.printMessage(Diagnostic.Kind.WARNING, "unsupported declaration type " + declarationBitfield.type());
                case Declaration.Variable declarationVariable -> {
                    switch (declarationVariable.kind()) {
                        case FIELD -> {
                            var node = parseVariable(declarationVariable, declarationVariable.type());
                            parentNode.addNode(node);
                        }
                        default ->
                                messager.printMessage(Diagnostic.Kind.WARNING, "unsupported declaration kind " + declarationVariable.kind() + " " + declarationVariable.pos());

                    }
                }
                case Declaration.Typedef declarationTypedef -> {
                    var node = parseVariable(declarationTypedef, declarationTypedef.type());
                    parentNode.addNode(node);
                }
                case Declaration.Function _ -> {
                }
                default ->
                        messager.printMessage(Diagnostic.Kind.WARNING, "unsupported declaration type " + declaration.name() + " " + declaration.pos());

            }
        }
        if (!parentNode.nodes().isEmpty() && parent.kind().equals(Declaration.Scoped.Kind.TOPLEVEL)) {
            var node = new NativeMemoryNode(PrettyName.getObjectName(parentNode.getName()) + "Constants", NodeType.ENUM, null, parent.pos().toString());
            node.nodes().addAll(parentNode.nodes());
            model.computeIfAbsent(Declaration.Scoped.Kind.ENUM, _ -> new ArrayList<>()).add(node);
        }
    }

    private NativeMemoryNode parseVariable(Declaration declaration, Type type) {
        if (type.isErroneous()) {
            printError(declaration, "type " + type + " is erroneous");
            return null;
        }
        switch (type) {
            case Type.Array typeArray -> {
                if (typeArray.elementType().isErroneous()) {
                    printError(declaration, "type " + type + " is erroneous");
                    return null;
                }
                return new NativeMemoryNode(declaration.name(), NodeType.VARIABLE, OriginalType.of(typeArray), declaration.pos().toString());
            }
            case Type.Primitive typePrimitive -> {
                if (declaration instanceof Declaration.Constant declarationConstant) {
                    return new NativeMemoryNode(declaration.name(), NodeType.VARIABLE, OriginalType.of(typePrimitive), declaration.pos().toString(), declarationConstant.value());
                } else {
                    return new NativeMemoryNode(declaration.name(), NodeType.VARIABLE, OriginalType.of(typePrimitive), declaration.pos().toString());
                }
            }
            case Type.Delegated typeDelegated -> {
                switch (typeDelegated.kind()) {
                    case SIGNED, UNSIGNED -> {
                        if (declaration instanceof Declaration.Constant declarationConstant) {
                            return new NativeMemoryNode(declaration.name(), NodeType.VARIABLE, OriginalType.of(typeDelegated), declaration.pos().toString(), declarationConstant.value());
                        } else {
                            return new NativeMemoryNode(declaration.name(), NodeType.VARIABLE, OriginalType.of(typeDelegated), declaration.pos().toString());
                        }
                    }
                    case POINTER, TYPEDEF -> {
                        return new NativeMemoryNode(declaration.name(), NodeType.VARIABLE, OriginalType.of(typeDelegated.type()), declaration.pos().toString());
                    }
                    default -> printWarning(declaration, "unsupported variable kind " + typeDelegated.kind());
                }
            }
            case Type.Declared typeDeclared -> {
                var node = new NativeMemoryNode(declaration.name(), NodeType.VARIABLE, OriginalType.of(typeDeclared), declaration.pos().toString());
                parseDeclaration(typeDeclared.tree(), node);
                return node;
            }
            default -> printWarning(declaration, "unknown field type " + type);
        }
        return null;
    }

    private NodeType getNodeType(Declaration.Scoped.Kind kind, Collection<Record> attributes) {
        var it = attributes.iterator();
        var isAnonymous = false;
        while (it.hasNext()) {
            var r = it.next();
            if (r.getClass().getSimpleName().equals("AnonymousStruct")) {
                isAnonymous = true;
                break;
            }
        }
        switch (kind) {
            case STRUCT -> {
                return isAnonymous ? NodeType.ANON_STRUCT : NodeType.STRUCT;
            }
            case ENUM -> {
                return isAnonymous ? NodeType.ANON_ENUM : NodeType.ENUM;
            }
            case UNION -> {
                return isAnonymous ? NodeType.ANON_UNION : NodeType.UNION;
            }
            default -> {
                return NodeType.VARIABLE;
            }
        }
    }

    private void printError(Declaration declaration, String message) {
        messager.printMessage(Diagnostic.Kind.ERROR, declaration.pos() + ": " + message);
    }

    private void printWarning(Declaration declaration, String message) {
        messager.printMessage(Diagnostic.Kind.WARNING, declaration.pos() + ": " + message);
    }


}

package org.digitalsmile.parser;

import org.openjdk.jextract.Declaration;
import org.openjdk.jextract.JextractTool;
import org.openjdk.jextract.Type;
import org.openjdk.jextract.impl.TypeImpl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Parser {
    private final Declaration.Scoped parsed;
    private final String structName;
    private final String packageName;
    private final String prettyName;
    private final Map<String, String> parsingStructsMap = new HashMap<>();

    private final NativeMemoryModel nativeMemoryModel;

    public static void main(String[] args) throws IOException {
        Declaration.Scoped parsed;
        try {
            parsed = JextractTool.parse(Path.of(args[0]));
        } catch (ExceptionInInitializerError e) {
            System.err.println(e.getMessage());
            return;
        }
        var packageName = args[1];
        var guid = args[2];
        var allStructsToBeParsed = Arrays.copyOfRange(args, 3, args.length);
        for (int i = 3; i < args.length; i++) {
            var struct = args[i].split(":");
            var structName = struct[0];
            var prettyName = struct[1];
            var parser = new Parser(parsed, structName, prettyName, packageName, allStructsToBeParsed);
            parser.parse(guid);
        }
    }

    public Parser(Declaration.Scoped parsed, String structName, String prettyName, String packageName, String[] allStructsToBeParsed) {
        this.parsed = parsed;
        this.structName = structName;
        this.packageName = packageName;
        this.prettyName = prettyName;
        this.nativeMemoryModel = new NativeMemoryModel(structName);
        for (String s : allStructsToBeParsed) {
            var structs = s.split(":");
            parsingStructsMap.put(structs[0], structs[1]);
        }
    }


    public void parse(String runId) throws IOException {
        for (Declaration declaration : parsed.members()) {
            if (declaration.name().equals(structName)) {
                var scoped = (Declaration.Scoped) declaration;
                parseField(scoped, nativeMemoryModel.getNodes());
            }
        }
        System.out.println(nativeMemoryModel);
        var tmpPath = System.getProperty("java.io.tmpdir");
        var directory = Files.createDirectories(Path.of(tmpPath, runId, packageName));
        var file = Files.createFile(Path.of(directory.toFile().getAbsolutePath(), prettyName));
        Files.write(file, StructComposer.compose(nativeMemoryModel, packageName, prettyName, parsingStructsMap::get).getBytes());
    }

    private void parseField(Declaration.Scoped parent, List<NativeMemoryNode> nodes) {
        var unionCount = 1;
        for (Declaration field : parent.members()) {
            if (field instanceof Declaration.Variable variable) {
                nodes.add(parseType(variable.type(), variable.name()));
            } else if (field instanceof Declaration.Scoped scoped) {
                switch (scoped.kind()) {
                    case UNION -> {
                        var unionNode = new NativeMemoryNode("union_" + unionCount, null);
                        parseField(scoped, unionNode.getNodes());
                        nodes.add(unionNode);
                    }
                    case null, default -> System.err.println("Unknown field type " + scoped.kind());
                }
            }
        }
    }

    private NativeMemoryNode parseType(Type type, String variableName) {
        switch (type) {
            case Type.Array typeArray -> {
                if (typeArray.elementType() instanceof Type.Delegated typeDelegated) {
                    var node = parseType(typeDelegated.type(), variableName);
                    System.err.println("Array: " + variableName + " " + node.getType());
                    var isPresent = typeArray.elementCount().isPresent();
                    var valueType = new TypeImpl.ArrayImpl(typeArray.kind(), isPresent ? typeArray.elementCount().getAsLong() : 0, node.getType());
                    return new NativeMemoryNode(variableName, valueType, isPresent ? typeArray.elementCount().getAsLong() : 0);
                } else {
                    var isPresent = typeArray.elementCount().isPresent();
                    return new NativeMemoryNode(variableName, type, isPresent ? typeArray.elementCount().getAsLong() : 0);
                }
            }
            case Type.Primitive typePrimitive -> {
                return new NativeMemoryNode(variableName, typePrimitive);
            }
            case Type.Delegated typeDelegated -> {
                switch (typeDelegated.kind()) {
                    case TYPEDEF, SIGNED, UNSIGNED -> {
                        var node = parseType(typeDelegated.type(), variableName);
                        System.err.println(variableName + " " + node.getType());
                        return new NativeMemoryNode(variableName, node.getType());
                    }
                    default ->
                            System.err.println("Unknown delegated field type '" + variableName + " (" + typeDelegated.kind() + "')");
                }
            }
            case Type.Declared typeDeclared -> {
                if (!parsingStructsMap.containsKey(typeDeclared.tree().name())) {
                    System.err.println(parsingStructsMap);
                    System.err.println("Field '" + variableName + " (" + typeDeclared.tree().name() + ")' is present in structure '" + structName + "', but not declared in annotation! Please, add this field explicitly");
                    System.exit(1);
                }
                return new NativeMemoryNode(variableName, type);
            }
            case null, default -> System.err.println("Unknown field type '" + variableName + " (" + type + "')");
        }
        return null;
    }
}

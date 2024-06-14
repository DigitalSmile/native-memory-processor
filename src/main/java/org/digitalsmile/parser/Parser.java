package org.digitalsmile.parser;

import jdk.jshell.spi.ExecutionControl;
import org.openjdk.jextract.Declaration;
import org.openjdk.jextract.JextractTool;
import org.openjdk.jextract.Type;
import org.openjdk.jextract.impl.TypeImpl;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;


public class Parser {
    private final Declaration.Scoped parsed;
    private final String packageName;
    private final boolean rootEnums;
    private final Map<String, String> parsingStructsMap = new HashMap<>();
    private final Map<String, String> parsingEnumsMap = new HashMap<>();

    private final String filePath;
    private final String fileName;

    public static void main(String[] args) throws IOException {
        var header = args[0];
        Declaration.Scoped parsed;
        try {
            parsed = JextractTool.parse(Path.of(header));
        } catch (ExceptionInInitializerError e) {
            System.err.println(e.getMessage());
            return;
        }
        var packageName = args[1];
        var guid = args[2];
        var rootEnums = args[3];
        var allEntitiesToBeParsed = Arrays.copyOfRange(args, 4, args.length);
        var parser = new Parser(parsed, packageName, header, rootEnums, allEntitiesToBeParsed);
        parser.parse(guid);
    }

    public Parser(Declaration.Scoped parsed, String packageName, String filePath, String rootEnums, String[] allEntitiesToBeParsed) {
        this.parsed = parsed;
        this.packageName = packageName;
        this.rootEnums = Boolean.parseBoolean(rootEnums);
        this.filePath = filePath;
        var fileBlocks = filePath.split(Pattern.quote(File.separator));
        this.fileName = fileBlocks[fileBlocks.length - 1].split("\\.")[0];
        for (String s : allEntitiesToBeParsed) {
            var entity = s.split(":");
            var typeAndName = entity[0].split("!");
            if (typeAndName[0].equals("S")) {
                parsingStructsMap.put(typeAndName[1], entity[1]);
            } else if (typeAndName[0].equals("E")) {
                parsingEnumsMap.put(typeAndName[1], entity[1]);
            }
        }
    }

    private String getPrettyName(Map<String, String> mapper, Declaration declaration) {
        if (mapper.isEmpty()) {
            return declaration.name();
        }
        var prettyName = mapper.get(declaration.name());
        if (prettyName == null) {
            System.err.println(declaration.name() + " not specified in map");
            return declaration.name();
        }
        return prettyName;
    }

    public void parse(String runId) throws IOException {
        var topLevelEnumModel = new NativeMemoryModel(fileName);
        //System.err.println("Ololo");
        //var included = parsed.members().stream().filter(declaration -> declaration.pos().path().toString().contains("vlc")).toList();
        for (Declaration declaration : parsed.members()) {
            var isDefinedInFile = declaration.pos().path().equals(Path.of(filePath));
            if (!isDefinedInFile) {
                continue;
            }
            if (declaration instanceof Declaration.Scoped declarationScoped) {
                switch (declarationScoped.kind()) {
                    case STRUCT -> {
                        if (!parsingStructsMap.isEmpty()) {
                            if (!parsingStructsMap.containsKey(declarationScoped.name())) {
                                continue;
                            }
                        }
                        var nativeMemoryModel = new NativeMemoryModel(fileName);
                        parseDeclaration(declarationScoped, nativeMemoryModel.getNodes());
                        var tmpPath = System.getProperty("java.io.tmpdir");
                        var directory = Files.createDirectories(Path.of(tmpPath, runId, packageName));
                        var file = Files.createFile(Path.of(directory.toFile().getAbsolutePath(), getPrettyName(parsingStructsMap, declarationScoped)));
                        Files.write(file, StructComposer.compose(nativeMemoryModel, packageName, getPrettyName(parsingStructsMap, declarationScoped), (structName) -> {
                            if (parsingStructsMap.isEmpty()) {
                                return structName;
                            }
                            return parsingStructsMap.get(structName);
                        }).getBytes());

                    }
                    case ENUM -> {
                        if (!parsingEnumsMap.isEmpty()) {
                            if (!parsingEnumsMap.containsKey(declarationScoped.name())) {
                                continue;
                            }
                        }
                        if (declarationScoped.name().isEmpty()) {
                            System.err.println(fileName + ": unsupported enum without variable declaration at " + declaration.pos());
                            continue;
                        }
                        var nativeMemoryModel = new NativeMemoryModel(fileName);
                        for (Declaration enumDeclaration : declarationScoped.members()) {
                            var constant = (Declaration.Constant) enumDeclaration;
                            if (constant.type() instanceof Type.Primitive) {
                                nativeMemoryModel.addNode(new NativeMemoryNode(constant.name(), constant.type(), constant.value()));
                            }
                        }
                        var tmpPath = System.getProperty("java.io.tmpdir");
                        var directory = Files.createDirectories(Path.of(tmpPath, runId, packageName));
                        var file = Files.createFile(Path.of(directory.toFile().getAbsolutePath(), getPrettyName(parsingEnumsMap, declarationScoped)));
                        Files.write(file, EnumComposer.compose(nativeMemoryModel, packageName, getPrettyName(parsingEnumsMap, declarationScoped)).getBytes());
                    }
                    default ->
                            System.err.println(declarationScoped.name() + ": unsupported declaration kind " + declarationScoped.kind());
                }
            } else if (declaration instanceof Declaration.Constant declarationConstant) {
                if (rootEnums) {
                    var node = parseVariable(declarationConstant.name(), declarationConstant.type());
                    topLevelEnumModel.addNode(new NativeMemoryNode(node.getName(), node.getType(), declarationConstant.value()));
                }
            } else if (declaration instanceof Declaration.Variable declarationVariable) {
                //System.err.println("VARIABLE: " + declarationVariable.name() + " " + declarationVariable.type());
            } else if (declaration instanceof Declaration.Typedef declarationTypedef) {
                //System.err.println(declarationTypedef.name() + " " + declarationTypedef.type());
            } else if (declaration instanceof  Declaration.Function declarationFunction) {
                //System.err.println(declarationFunction);
            } else if (declaration instanceof Declaration.Bitfield declarationBitfield) {
                //System.err.println(declarationBitfield);
            }
        }
        if (rootEnums) {
            var tmpPath = System.getProperty("java.io.tmpdir");
            var directory = Files.createDirectories(Path.of(tmpPath, runId, packageName));
            var file = Files.createFile(Path.of(directory.toFile().getAbsolutePath(), PrettyName.getFileName(fileName)));
            Files.write(file, EnumComposer.compose(topLevelEnumModel, packageName, PrettyName.getFileName(fileName)).getBytes());
        }
    }

    private void parseDeclaration(Declaration.Scoped parent, List<NativeMemoryNode> nodes) {
        var unionCount = 1;
        for (Declaration field : parent.members()) {
            if (field instanceof Declaration.Variable variable) {
                nodes.add(parseVariable(variable.name(), variable.type()));
            } else if (field instanceof Declaration.Scoped scoped) {
                switch (scoped.kind()) {
                    case UNION -> {
                        var unionNode = new NativeMemoryNode("union_" + unionCount++, null);
                        parseDeclaration(scoped, unionNode.getNodes());
                        nodes.add(unionNode);
                    }
                    case null, default -> System.err.println("Unknown field type " + scoped.kind());
                }
            } else if (field instanceof Declaration.Typedef declarationTypedef) {
               // System.err.println(declarationTypedef.name());
            }
        }
    }

    private NativeMemoryNode parseVariable(String variableName, Type type) {
        switch (type) {
            case Type.Array typeArray -> {
                if (typeArray.elementType() instanceof Type.Delegated typeDelegated) {
                    var node = parseVariable(variableName, typeDelegated.type());
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
                        var node = parseVariable(variableName, typeDelegated.type());
                        return new NativeMemoryNode(variableName, node.getType());
                    }
                    case POINTER -> {
                        //System.err.println("Trololo " + variableName + " " + typeDelegated.type());
                        var node = parseVariable(variableName, typeDelegated.type());
                        return new NativeMemoryNode(variableName, node.getType());
                    }
                    default ->
                            System.err.println("Unknown delegated field type '" + variableName + " (" + typeDelegated.kind() + "')");
                }
            }
            case Type.Declared typeDeclared -> {
                if (!parsingStructsMap.containsKey(typeDeclared.tree().name())) {
                    var isPresentInParsed = parsed.members().stream().anyMatch(declaration -> declaration.name().equals(typeDeclared.tree().name()));
                    if (!isPresentInParsed) {
                        System.err.println("Field '" + variableName + " (" + typeDeclared.tree().name() + ")' is present in file '" + filePath + "', but not declared in annotation! Please, add this field explicitly");
                        System.exit(1);
                    }
                }
                return new NativeMemoryNode(variableName, type);
            }
            case null, default -> System.err.println("Unknown field type '" + variableName + " (" + type + "')");
        }
        return null;
    }
}

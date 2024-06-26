package io.github.digitalsmile.parser;

import io.github.digitalsmile.ParsingOption;
import io.github.digitalsmile.composers.EnumComposer;
import io.github.digitalsmile.composers.StructComposer;
import org.openjdk.jextract.Declaration;
import org.openjdk.jextract.JextractTool;
import org.openjdk.jextract.Type;
import org.openjdk.jextract.impl.TypeImpl;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;


public class Parser {
    private final Declaration.Scoped parsed;
    private final String packageName;
    private final boolean rootEnums;
    private final Map<String, String> parsingStructsMap;
    private final Map<String, String> parsingEnumsMap;

    private final String filePath;
    private final String fileName;

    private final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(System.out));

    public static void main(String[] args) throws IOException {
        var properties = new Properties();
        properties.load(System.in);

        var header = properties.getProperty(ParsingOption.HEADER_FILE.getOption());
        Declaration.Scoped parsed;
        try {
            parsed = JextractTool.parse(Path.of(header));
        } catch (ExceptionInInitializerError e) {
            sendError(e.getMessage());
            return;
        }
        var packageName = properties.getProperty(ParsingOption.PACKAGE_NAME.getOption());
        var rootEnums = properties.getProperty(ParsingOption.ROOT_ENUM_CREATION.getOption());
        ;

        var keys = properties.keys();
        Map<String, String> parsingStructsMap = new HashMap<>();
        Map<String, String> parsingEnumsMap = new HashMap<>();
        while (keys.hasMoreElements()) {
            var key = (String) keys.nextElement();
            var value = (String) properties.get(key);
            if (key.startsWith("Structs.")) {
                parsingStructsMap.put(key.split("Structs.")[1], value);
            } else if (key.startsWith("Enums.")) {
                parsingEnumsMap.put(key.split("Enums.")[1], value);
            } else if (key.equals("Structs")) {
                parsingStructsMap.put("Structs", value);
            } else if (key.equals("Enums")) {
                parsingEnumsMap.put("Enums", value);
            }
        }
        var parser = new Parser(parsed, packageName, header, rootEnums, parsingStructsMap, parsingEnumsMap);
        parser.parse();
    }

    public Parser(Declaration.Scoped parsed, String packageName, String filePath, String rootEnums, Map<String, String> parsingStructsMap, Map<String, String> parsingEnumsMap) {
        this.parsed = parsed;
        this.packageName = packageName;
        this.rootEnums = Boolean.parseBoolean(rootEnums);
        this.filePath = filePath;
        var fileBlocks = filePath.split(Pattern.quote(File.separator));
        this.fileName = fileBlocks[fileBlocks.length - 1].split("\\.")[0];
        this.parsingStructsMap = parsingStructsMap;
        this.parsingEnumsMap = parsingEnumsMap;
    }

    private String getPrettyName(Map<String, String> mapper, Declaration declaration) {
        if (mapper.isEmpty()) {
            return declaration.name();
        }
        var prettyName = mapper.get(declaration.name());
        if (prettyName == null) {
            return declaration.name();
        }
        return prettyName;
    }

    public void parse() throws IOException {
        var allStructs = parsingStructsMap.get("Structs");
        boolean parseAllStructs = false;
        if (allStructs != null) {
            parseAllStructs = true;
            parsingStructsMap.clear();
        }
        var allEnums = parsingEnumsMap.get("Enums");
        boolean parseAllEnums = false;
        if (allEnums != null) {
            parseAllEnums = true;
            parsingEnumsMap.clear();
        }
        var topLevelEnumModel = new NativeMemoryModel(fileName);
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
                        } else if (!parseAllStructs) {
                            continue;
                        }
                        var nativeMemoryModel = new NativeMemoryModel(fileName);
                        parseDeclaration(declarationScoped, nativeMemoryModel.getNodes());
                        var prettyName = getPrettyName(parsingStructsMap, declarationScoped);
                        writer.write("fileName: " + prettyName + "\n");
                        writer.write(StructComposer.compose(nativeMemoryModel, packageName, prettyName, (structName) -> {
                            if (parsingStructsMap.isEmpty()) {
                                return structName;
                            }
                            return parsingStructsMap.get(structName);
                        }));
                        writer.write("===\n");
                        writer.flush();
                    }
                    case ENUM -> {
                        if (!parsingEnumsMap.isEmpty()) {
                            if (!parsingEnumsMap.containsKey(declarationScoped.name())) {
                                continue;
                            }
                        } else if (!parseAllEnums) {
                            continue;
                        }
                        if (declarationScoped.name().isEmpty()) {
                            sendWarning(declarationScoped, "unsupported enum without variable declaration");
                            continue;
                        }
                        var nativeMemoryModel = new NativeMemoryModel(fileName);
                        for (Declaration enumDeclaration : declarationScoped.members()) {
                            var constant = (Declaration.Constant) enumDeclaration;
                            if (constant.type() instanceof Type.Primitive) {
                                nativeMemoryModel.addNode(new NativeMemoryNode(constant.name(), constant.type(), constant.value()));
                            }
                        }
                        var prettyName = getPrettyName(parsingEnumsMap, declarationScoped);
                        writer.write("fileName: " + prettyName + "\n");
                        writer.write(EnumComposer.compose(nativeMemoryModel, packageName, prettyName));
                        writer.write("===\n");
                        writer.flush();
                    }
                    default -> sendWarning(declarationScoped, "unsupported declaration kind " + declarationScoped.kind());
                }
            } else if (declaration instanceof Declaration.Constant declarationConstant) {
                if (rootEnums) {
                    var node = parseVariable(declaration, declarationConstant.name(), declarationConstant.type());
                    topLevelEnumModel.addNode(new NativeMemoryNode(node.getName(), node.getType(), declarationConstant.value()));
                }
            } else if (declaration instanceof Declaration.Variable declarationVariable) {
                sendWarning(declarationVariable, "unsupported declaration type " + declarationVariable.type());
            } else if (declaration instanceof Declaration.Typedef declarationTypedef) {
                sendWarning(declarationTypedef, "unsupported declaration type " + declarationTypedef.type());
            } else if (declaration instanceof Declaration.Function declarationFunction) {
                sendWarning(declarationFunction, "unsupported declaration type " + declarationFunction.type());
            } else if (declaration instanceof Declaration.Bitfield declarationBitfield) {
                sendWarning(declarationBitfield, "unsupported declaration type " + declarationBitfield.type());
            }
        }
        if (rootEnums) {
            var prettyName = PrettyName.getFileName(fileName);
            writer.write("fileName: " + prettyName + "\n");
            writer.write(EnumComposer.compose(topLevelEnumModel, packageName, prettyName));
            writer.write("===\n");
            writer.flush();
        }
    }

    private void parseDeclaration(Declaration.Scoped parent, List<NativeMemoryNode> nodes) {
        var unionCount = 1;
        for (Declaration field : parent.members()) {
            if (field instanceof Declaration.Variable variable) {
                nodes.add(parseVariable(parent, variable.name(), variable.type()));
            } else if (field instanceof Declaration.Scoped scoped) {
                switch (scoped.kind()) {
                    case UNION -> {
                        var unionNode = new NativeMemoryNode("union_" + unionCount++, null);
                        parseDeclaration(scoped, unionNode.getNodes());
                        nodes.add(unionNode);
                    }
                    case null, default -> sendWarning(scoped, "unsupported declaration kind " + scoped.kind());
                }
            } else if (field instanceof Declaration.Typedef declarationTypedef) {
                sendWarning(declarationTypedef, "unsupported declaration type " + declarationTypedef.type());
            }
        }
    }

    private NativeMemoryNode parseVariable(Declaration scoped, String variableName, Type type) {
        switch (type) {
            case Type.Array typeArray -> {
                if (typeArray.elementType() instanceof Type.Delegated typeDelegated) {
                    var node = parseVariable(scoped, variableName, typeDelegated.type());
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
                    case TYPEDEF, SIGNED, UNSIGNED, POINTER -> {
                        var node = parseVariable(scoped, variableName, typeDelegated.type());
                        return new NativeMemoryNode(variableName, node.getType());
                    }
                    default -> sendWarning(scoped, "unsupported variable type " + typeDelegated.kind());
                }
            }
            case Type.Declared typeDeclared -> {
                if (!parsingStructsMap.containsKey(typeDeclared.tree().name())) {
                    var isPresentInParsed = parsed.members().stream().anyMatch(declaration -> declaration.name().equals(typeDeclared.tree().name()));
                    if (!isPresentInParsed) {
                        sendError(scoped, "field '" + variableName + "' is present in file, but not declared in annotation! Please, add this field explicitly");
                        System.exit(1);
                    }
                }
                return new NativeMemoryNode(variableName, type);
            }
            case null, default -> sendWarning(scoped, "unknown field type '" + variableName + " (" + type + "')");
        }
        return null;
    }

    static void sendError(String message) {
        System.err.println("Error:" + message);
    }
    static void sendError(Declaration declaration, String message) {
        System.err.println("Error:" + format(declaration.pos().toString(), declaration.name(), message));
    }
    static void sendWarning(Declaration declaration, String message) {
        System.err.println("Warning:" + format(declaration.pos().toString(), declaration.name(), message));
    }
    static void sendDebug(String message) {
        System.err.println("Debug:" + message);
    }

    private static String format(String position, String name, String message) {
        if (!name.isEmpty()) {
            name = " - " + name;
        }
        return "'" + position + "'" + name + ": " + message;
    }
}

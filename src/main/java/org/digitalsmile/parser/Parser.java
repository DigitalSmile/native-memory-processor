package org.digitalsmile.parser;

import org.openjdk.jextract.Declaration;
import org.openjdk.jextract.JextractTool;
import org.openjdk.jextract.Type;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;


public class Parser {
    private final StructRecord structRecord = new StructRecord();

    public void parse(Path headerPath, String structName, String prettyName) throws IOException {
        Declaration.Scoped parsed;
        try {
            parsed = JextractTool.parse(List.of(headerPath));
        } catch (ExceptionInInitializerError e) {
            System.err.println(e.getCause());
            return;
        }
        structRecord.setPackageName("org.digitalsmile.runj");
        structRecord.setRecordName(prettyName);
        for (Declaration declaration : parsed.members()) {
            if (declaration.name().equals(structName)) {
                var scoped = (Declaration.Scoped) declaration;
                var attrs = scoped.attributes();
                for (Record record : attrs) {
                    System.out.println(record);
                }
                parseField(scoped);
                //arguments = String.join(", ",argumentsList.toArray(String[]::new));
            }
        }
        Files.writeString(Path.of(Path.of("src/main/java/org/digitalsmile/runj/") + File.separator + prettyName + ".java"), structRecord.compileTemplate());
        //System.out.println(structRecord.compileTemplate());

    }

    private void parseField(Declaration.Scoped parent) {
        for (Declaration field : parent.members()) {
            var variable = (Declaration.Variable) field;

            parseType(variable.type(), 0, variable.name());
        }
    }

    private void parseType(Type type, long size, String variableName) {
        if (type instanceof Type.Array typeArray) {
            var isPresent = typeArray.elementCount().isPresent();
            parseType(typeArray.elementType(), isPresent ? typeArray.elementCount().getAsLong() : 0, variableName);
        } else if (type instanceof Type.Primitive typePrimitive) {
            switch (typePrimitive.kind()) {
                case Char -> {
                    structRecord.addArgument("byte[] " + variableName);
                    structRecord.addEmptyArgument("new byte[]{0}");
                    structRecord.addLayoutMember("MemoryLayout.sequenceLayout(" + size + ", ValueLayout.JAVA_BYTE).withName(\"" + variableName + "\")");
                    structRecord.addLayoutMethodHandle("private static final MethodHandle MH_" + variableName.toUpperCase() + " = LAYOUT.sliceHandle(MemoryLayout.PathElement.groupElement(\"" + variableName + "\"));");
                    structRecord.addGettersArgument("invokeExact(MH_" + variableName.toUpperCase() + ", buffer).toArray(ValueLayout.JAVA_BYTE)");

                    var setter = StructTemplate.ARRAY_SETTER_TEMPLATE.replace("${variableNameUpperCase}", variableName.toUpperCase());
                    setter = setter.replace("${variableName}", variableName);
                    setter = setter.replace("${typeUpperCase}", "JAVA_BYTE");
                    structRecord.addSettersArgument(setter);
                }
                case Int -> {
                    structRecord.addArgument("int " + variableName);
                    structRecord.addEmptyArgument("0");
                    structRecord.addLayoutMember("ValueLayout.JAVA_INT.withName(\"" + variableName + "\")");
                    structRecord.addLayoutVarHandle("private static final VarHandle VH_" + variableName.toUpperCase() + " = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement(\"" + variableName + "\"));");
                    structRecord.addGettersArgument("(int) VH_" + variableName.toUpperCase() + ".get(buffer, 0L)");
                    structRecord.addSettersArgument("\tVH_" + variableName.toUpperCase() + ".set(buffer, 0L, " + variableName + ");");
                }
            }
        } else if (type instanceof Type.Delegated typeDelegated) {
            switch (typeDelegated.kind()) {
                case TYPEDEF, SIGNED, UNSIGNED -> parseType(typeDelegated.type(), 0, variableName);
            }
        }
    }
}

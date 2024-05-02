package org.digitalsmile.parser;

import java.util.ArrayList;
import java.util.List;

public class StructRecord {
    private String packageName;
    private String recordName;
    private final List<String> arguments = new ArrayList<>();
    private final List<String> emptyArguments = new ArrayList<>();


    private final List<String> layoutMembers = new ArrayList<>();
    private final List<String> layoutVarHandles = new ArrayList<>();
    private final List<String> layoutMethodHandles = new ArrayList<>();

    private final List<String> preprocessGetters = new ArrayList<>();
    private final List<String> gettersArguments = new ArrayList<>();

    private final List<String> settersArguments = new ArrayList<>();

    private final List<String> structToString = new ArrayList<>();

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public void setRecordName(String recordName) {
        this.recordName = recordName;
    }

    public void addArgument(String argument) {
        this.arguments.add(argument);
    }

    public void addEmptyArgument(String argument) {
        this.emptyArguments.add(argument);
    }

    public void addLayoutMember(String layoutMember) {
        this.layoutMembers.add(layoutMember);
    }

    public void addLayoutVarHandle(String layoutVarHandle) {
        this.layoutVarHandles.add(layoutVarHandle);
    }

    public void addLayoutMethodHandle(String layoutMethodHandle) {
        this.layoutMethodHandles.add(layoutMethodHandle);
    }

    public void addPreprocessGetter(String preprocessGetter) {
        this.preprocessGetters.add(preprocessGetter);
    }

    public void addGettersArgument(String gettersArgument) {
        this.gettersArguments.add(gettersArgument);
    }

    public void addSettersArgument(String settersArgument) {
        this.settersArguments.add(settersArgument);
    }

    public void addStructToString(String structToString) {
        this.structToString.add(structToString);
    }

    public String compileTemplate() {
        var impl = StructTemplate.TEMPLATE.replace("${packageName}", packageName);
        impl = impl.replace("${javaName}", recordName);
        impl = impl.replace("${arguments}", String.join(", ", arguments.toArray(String[]::new)));
        impl = impl.replace("${layoutMembers}", String.join(",\n\t\t", layoutMembers.toArray(String[]::new)));
        impl = impl.replace("${layoutVarHandles}", String.join("\n\t", layoutVarHandles.toArray(String[]::new)));
        impl = impl.replace("${layoutMethodHandles}", String.join("\n\t", layoutMethodHandles.toArray(String[]::new)));
        impl = impl.replace("${emptyArguments}", String.join(", ", emptyArguments.toArray(String[]::new)));
        impl = impl.replace("${preprocessGetters}", String.join("", preprocessGetters.toArray(String[]::new)));
        impl = impl.replace("${gettersArguments}", String.join(",\n\t\t\t", gettersArguments.toArray(String[]::new)));
        impl = impl.replace("${settersArguments}", String.join("\n\t", settersArguments.toArray(String[]::new)));
        return impl;
    }
}

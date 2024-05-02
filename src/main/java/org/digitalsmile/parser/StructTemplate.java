package org.digitalsmile.parser;

public class StructTemplate {
    public static final String TEMPLATE = """
                        package ${packageName};

                        import org.digitalsmile.NativeMemoryLayout;

                        import java.lang.foreign.MemoryLayout;
                        import java.lang.foreign.MemorySegment;
                        import java.lang.foreign.ValueLayout;
                        import java.lang.invoke.MethodHandle;
                        import java.lang.invoke.VarHandle;

                        public record ${javaName}(${arguments}) implements NativeMemoryLayout {

                            public static final MemoryLayout LAYOUT = MemoryLayout.structLayout(
                                ${layoutMembers}
                            );

                            ${layoutVarHandles}
                            ${layoutMethodHandles}

                            public static ${javaName} createEmpty() {
                                return new ${javaName}(${emptyArguments});
                            }

                            @SuppressWarnings("unchecked")
                            @Override
                            public ${javaName} fromBytes(MemorySegment buffer) throws Throwable {
                                ${preprocessGetters}
                                return new ${javaName}(
                                    ${gettersArguments}
                                );
                            }

                            @Override
                            public void toBytes(MemorySegment buffer) throws Throwable {
                                ${settersArguments}
                            }

                            private static MemorySegment invokeExact(MethodHandle handle, MemorySegment buffer) throws Throwable {
                                return ((MemorySegment) handle.invokeExact(buffer, 0L));
                            }
                        }
                        """;
    public static final String ARRAY_SETTER_TEMPLATE = """
                var ${variableName}Tmp = invokeExact(MH_${variableNameUpperCase}, buffer);
                    for (int i = 0; i < ${variableName}.length; i++) {
                        ${variableName}Tmp.setAtIndex(ValueLayout.${typeUpperCase}, i, ${variableName}[i]);
                    }
            """;
}

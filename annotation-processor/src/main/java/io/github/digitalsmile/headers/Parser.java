package io.github.digitalsmile.headers;

import io.github.digitalsmile.NativeProcessor;
import io.github.digitalsmile.headers.model.NativeMemoryModel;
import io.github.digitalsmile.headers.model.NativeMemoryNode;
import io.github.digitalsmile.headers.mapping.OriginalType;
import org.openjdk.jextract.Declaration;

import javax.annotation.processing.Messager;
import javax.tools.Diagnostic;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class Parser {


    private final Messager messager;

    public Parser(Messager messager) {
        this.messager = messager;
    }

    public List<NativeMemoryModel> parse(List<NativeProcessor.Type> structs, List<NativeProcessor.Type> enums,
                                         List<NativeProcessor.Type> unions,
                                         Map<Path, Declaration.Scoped> parsed) {
        List<String> parsedObjectNames = new ArrayList<>();
        for (Declaration.Scoped declaration : parsed.values()) {
            parsedObjectNames.addAll(flatten(declaration.members()).toList());
        }
        for (String object : parsedObjectNames) {
            OriginalType.register(object);
        }

        List<NativeMemoryModel> models = new ArrayList<>();
        for (Map.Entry<Path, Declaration.Scoped> entry : parsed.entrySet()) {
            var declarationParser = new DeclarationParser(messager, entry.getKey(), structs, enums, unions);
            var root = new NativeMemoryNode(entry.getKey().toFile().getName().split("\\.")[0], null, null, entry.getValue().pos().toString());
            declarationParser.parseDeclaration(entry.getValue(), root);
            models.add(new NativeMemoryModel(entry.getKey(), declarationParser.getModel()));
        }
        return models;
    }

    private static Stream<String> flatten(final List<Declaration> nodes) {
        return nodes
                .stream()
                .flatMap(node -> {
                    if (node instanceof Declaration.Scoped scoped) {
                        return Stream.of(scoped.name());
                    } else if (node instanceof Declaration.Constant constant) {
                        return Stream.of(constant.name());
                    }
                    return Stream.empty();
                });
    }


}

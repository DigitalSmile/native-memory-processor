package io.github.digitalsmile.headers.model;

import org.openjdk.jextract.Declaration;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public record NativeMemoryModel(Path path, Map<Declaration.Scoped.Kind, List<NativeMemoryNode>> nodes) {
}

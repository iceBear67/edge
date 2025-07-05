package io.ib67.edge.script.locator;

import lombok.SneakyThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

public class DirectoryModuleLocator implements ModuleLocator {
    protected final Path searchRoot;

    public DirectoryModuleLocator(Path searchRoot) {
        this.searchRoot = searchRoot;
    }

    @Override
    @SneakyThrows
    public Set<String> discoverModules() {
        try (var files = Files.list(searchRoot)) {
            return files.map(it -> it.getFileName().toString()).collect(Collectors.toSet());
        }
    }

    @Override
    public Path locateRoot(String module) {
        var root = searchRoot.resolve(module);
        if (!root.normalize().startsWith(searchRoot)) {
            throw new IllegalArgumentException("Path to " + module + " out of search root.");
        }
        if (Files.exists(root)) {
            return root;
        }
        throw new IllegalArgumentException("Path to " + module + " not found.");
    }

    @Override
    public Path locateModule(String module, String file) {
        var path = locateRoot(module).resolve(file);
        if (!path.normalize().startsWith(searchRoot)) {
            throw new IllegalArgumentException("Path to @" + module + "/" + file + " is out of search root.");
        }
        return path;
    }
}

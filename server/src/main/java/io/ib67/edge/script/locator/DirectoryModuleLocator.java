/*
 *    Copyright 2025 iceBear67 and Contributors
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

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

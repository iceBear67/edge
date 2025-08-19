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

package io.ib67.edge.script.io;

import io.ib67.edge.script.locator.ModuleLocator;
import org.graalvm.polyglot.io.FileSystem;

import java.net.URI;
import java.nio.file.Path;
import java.util.function.Supplier;

/**
 * This asserts the library locator can be used on delegated fs
 */
public class ESModuleFS extends DelegatedFileSystem {
    protected final Supplier<ModuleLocator> locator;

    // the underlying implementation of this library locator can be a thread local.
    public ESModuleFS(FileSystem delegatedFS, Supplier<ModuleLocator> locator) {
        super(delegatedFS);
        this.locator = locator;
    }

    @Override
    public Path parsePath(URI uri) {
        return super.parsePath(uri);
    }

    // inputs:
    // @XXX/xxxx
    // xxxx
    @Override
    public Path parsePath(String path) {
        // we assume that this parsePath method will run on the same thread the script run.
        var locator = this.locator.get();
        if (!path.isEmpty() && path.charAt(0) == '@') {
            var moduleName = path.substring(1);
            if (moduleName.isEmpty()) {
                throw new IllegalArgumentException("module name cannot be empty");
            }
            path = path.substring(1);

            var firstSlash = path.indexOf('/');
            if (firstSlash == -1) {
                var root = locator.locateRoot(path);
                if (root == null) {
                    throw new IllegalArgumentException("cannot find root for module "+path);
                }
                return root;
            }
            moduleName = path.substring(0, firstSlash);
            var finalPath = locator.locateModule(moduleName, path.substring(firstSlash + 1));
            if (finalPath == null) {
                throw new IllegalArgumentException("path '" + path.substring(firstSlash + 1) + "' in module " + moduleName + " not found");
            }
            var moduleRoot = locator.locateRoot(moduleName);
            finalPath = finalPath.normalize();
            if (!finalPath.startsWith(moduleRoot)) {
                throw new IllegalArgumentException("invalid module path: " + finalPath);
            }
            return finalPath;
        }
        return super.parsePath(path);
    }
}

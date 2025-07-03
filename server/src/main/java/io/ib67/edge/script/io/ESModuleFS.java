package io.ib67.edge.script.io;

import io.ib67.edge.script.locator.LibraryLocator;
import org.graalvm.polyglot.io.FileSystem;

import java.net.URI;
import java.nio.file.Path;
import java.util.function.Supplier;

/**
 * This asserts the library locator can be used on delegated fs
 * todo refactor?
 */
public class ESModuleFS extends DelegatedFileSystem {
    protected final Supplier<LibraryLocator> locator;

    // the underlying implementation of this library locator can be a thread local.
    public ESModuleFS(FileSystem delegatedFS, Supplier<LibraryLocator> locator) {
        super(delegatedFS);
        this.locator = locator;
    }

    @Override
    public Path parsePath(URI uri) {
        return super.parsePath(uri);
    }

    //todo check ESM specification
    @Override
    public Path parsePath(String path) {
        // we assume that this parsePath method will run on the same thread the script run.
        var locator = this.locator.get();
        if (!path.isEmpty() && path.charAt(0) == '@') {
            var moduleName = path.substring(1);
            if (moduleName.isEmpty()) {
                throw new IllegalArgumentException("module name cannot be empty");
            }
            var moduleRoot = locator.locateRoot(path.substring(1));
            var firstSlash = path.indexOf('/');
            if (firstSlash == -1) {
                //todo import {} from "@vertx" 的情况下从哪里搜索 import？
                return moduleRoot;
            }
            var finalPath = locator.locateModule(moduleName, path.substring(firstSlash)).toAbsolutePath();
            if (!finalPath.startsWith(moduleRoot)) {
                throw new IllegalArgumentException("invalid module path: " + finalPath);
            }
            return finalPath;
        }
        return super.parsePath(path);
    }
}

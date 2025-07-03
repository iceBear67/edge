package io.ib67.edge.script.locator;

import java.nio.file.Path;
import java.util.Set;

public interface LibraryLocator {
    Set<String> discoverLibraries();

    /**
     * @throws IllegalArgumentException if it cannot find the root
     */
    Path locateRoot(String module);

    /**
     * @throws IllegalArgumentException if it cannot find the file
     */
    Path locateModule(String module, String file);
}

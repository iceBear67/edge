package io.ib67.edge.script.locator;

import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import com.google.common.jimfs.Jimfs;
import static org.junit.jupiter.api.Assertions.*;

class DirectoryModuleLocatorTest {
    private FileSystem imfs;
    private Path searchRoot;

    @BeforeEach
    void setup() {
        imfs = Jimfs.newFileSystem();
        searchRoot = imfs.getPath("/testRoot");
    }

    @AfterEach
    @SneakyThrows
    void teardown() {
        imfs.close();
    }

    @Test
    void testDiscoverModules() throws Exception {
        Files.createDirectory(searchRoot);
        Files.createFile(searchRoot.resolve("lib1"));
        Files.createFile(searchRoot.resolve("lib2"));

        DirectoryModuleLocator locator = new DirectoryModuleLocator(searchRoot);
        Set<String> libraries = locator.discoverModules();

        assertEquals(2, libraries.size());
        assertTrue(libraries.contains("lib1"));
        assertTrue(libraries.contains("lib2"));
    }

    @Test
    void testLocateRoot() throws Exception {
        Files.createDirectory(searchRoot);
        Path modulePath = searchRoot.resolve("module1");
        Files.createDirectory(modulePath);

        DirectoryModuleLocator locator = new DirectoryModuleLocator(searchRoot);
        Path locatedPath = locator.locateRoot("module1");

        assertEquals(modulePath, locatedPath);
    }

    @Test
    void testLocateRootThrowsWhenOutOfRoot() {
        DirectoryModuleLocator locator = new DirectoryModuleLocator(searchRoot);
        assertThrows(IllegalArgumentException.class, () -> locator.locateRoot("../module1"));
    }

    @Test
    void testLocateRootThrowsWhenNotFound() {
        DirectoryModuleLocator locator = new DirectoryModuleLocator(searchRoot);
        assertThrows(IllegalArgumentException.class, () -> locator.locateRoot("nonexistent"));
    }

    @Test
    void testLocateModule() throws Exception {
        Files.createDirectory(searchRoot);
        Path modulePath = searchRoot.resolve("module1");
        Files.createDirectory(modulePath);
        Path filePath = modulePath.resolve("file.txt");
        Files.createFile(filePath);

        DirectoryModuleLocator locator = new DirectoryModuleLocator(searchRoot);
        Path locatedPath = locator.locateModule("module1", "file.txt");

        assertEquals(filePath, locatedPath);
    }

    @Test
    void testLocateModuleThrowsWhenOutOfRoot() throws Exception {
        Files.createDirectory(searchRoot);
        Path modulePath = searchRoot.resolve("module1");
        Files.createDirectory(modulePath);

        DirectoryModuleLocator locator = new DirectoryModuleLocator(searchRoot);
        assertThrows(IllegalArgumentException.class, () -> locator.locateModule("module1", "../../file.txt"));
    }
}

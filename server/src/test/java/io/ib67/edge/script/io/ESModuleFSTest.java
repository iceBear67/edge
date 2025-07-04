package io.ib67.edge.script.io;

import com.google.common.jimfs.Jimfs;
import io.ib67.edge.script.locator.LibraryLocator;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.FileSystem;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ESModuleFSTest {
    FileSystem imfs;

    @BeforeEach
    void setup() {
        imfs = Jimfs.newFileSystem();
    }

    @AfterEach
    @SneakyThrows
    void teardown() {
        imfs.close();
    }

    @Test
    public void testESModuleFS() {
        var libLocator = mock(LibraryLocator.class);
        when(libLocator.locateRoot("test")).thenAnswer(it -> Path.of("/test"));
        when(libLocator.locateModule("test", "pathA")).thenAnswer(it -> Path.of("/pathA"));
        when(libLocator.locateModule("test", "pathB")).thenAnswer(it -> Path.of("/test/pathB"));
        var esmfs = new ESModuleFS(
                org.graalvm.polyglot.io.FileSystem.newReadOnlyFileSystem(
                        org.graalvm.polyglot.io.FileSystem.newFileSystem(imfs)
                ), () -> libLocator);
        assertThrows(IllegalArgumentException.class, () -> esmfs.parsePath("@"));
        assertThrows(IllegalArgumentException.class, () -> esmfs.parsePath("@notExist"));
        assertEquals("/test", esmfs.parsePath("@test").toString());
        assertThrows(IllegalArgumentException.class, () -> esmfs.parsePath("@test/pathA"));
        assertThrows(IllegalArgumentException.class, () -> esmfs.parsePath("@test/pathNotExist"));
        assertEquals("/test/pathB", esmfs.parsePath("@test/pathB").toString());

    }
}
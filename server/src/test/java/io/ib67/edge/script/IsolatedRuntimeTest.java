package io.ib67.edge.script;

import com.google.common.jimfs.Jimfs;
import io.ib67.edge.script.locator.DirectoryLibraryLocator;
import lombok.SneakyThrows;
import org.graalvm.polyglot.*;
import org.graalvm.polyglot.io.IOAccess;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class IsolatedRuntimeTest {
    static FileSystem jimfs;
    static Engine engine;
    static Path libraryRoot;

    @BeforeAll
    @SneakyThrows
    static void setupJimfs() {
        jimfs = Jimfs.newFileSystem();
        engine = Engine.newBuilder().in(InputStream.nullInputStream()).out(OutputStream.nullOutputStream()).build();
        libraryRoot = jimfs.getPath("/lib");
        Files.createDirectories(libraryRoot);
    }

    @SneakyThrows
    @AfterAll
    static void teardownJimfs() {
        jimfs.close();
    }

    @Test
    @SneakyThrows
    void test() {
        // make some privileged code
        var outputPath = jimfs.getPath("/privilegedIO");
        var privilegedCode = """
                const System = Java.type("java.lang.System");
                System.out.println(1);
                const Files = Java.type("java.nio.file.Files");
                const Path = Java.type("java.nio.file.Path");
                Files.writeString(outputPath, "hello");
                """;
        // let's validate that privileged code _actually_ works
        var _privileged_context = Context.newBuilder()
                .allowIO(IOAccess.ALL)
                .allowHostAccess(HostAccess.ALL)
                .allowHostClassLookup(className -> true)
                .build();
        _privileged_context.getBindings("js").putMember("outputPath", outputPath);
        _privileged_context.eval(Source.create("js", privilegedCode));
        _privileged_context.close(true);
        assertEquals("hello", Files.readString(outputPath));
        Files.deleteIfExists(outputPath);

        // then write it to the library
        var libraryCode = "export function writePrivileged(){%s}".formatted(privilegedCode);
        var libPath = libraryRoot.resolve("the_lib");
        Files.createDirectories(libPath);
        Files.writeString(libPath.resolve("index.mjs"), libraryCode);

        // create runtime and it will scan that library folder
        var runtime = new IsolatedRuntime(jimfs, new DirectoryLibraryLocator(libraryRoot));

        // ... and the "privileged code" should NOT work
        {
            // ignore the "unclosed" autocloseable warning.
            var context = runtime.create(Source.create("js", ""), it -> it);
            context.getScriptContext().getBindings("js").putMember("outputPath", libPath);
            assertThrows(PolyglotException.class, () -> context.eval(Source.create("js", privilegedCode)));
        }
        if (true) {
            var ctx = runtime.create(Source.create("js", ""), it -> it);
            ctx.getScriptContext().getBindings("js").putMember("outputPath", libPath);
        }
        var callLibrarySource = Source.newBuilder("js", """
                //import { writePrivileged } from "@the_lib/index.mjs";
                scope.the_lib.writePrivileged()
                """, "test.mjs").build();
        runtime.create(callLibrarySource, it -> it).close();
        assertTrue(Files.exists(outputPath));
        assertEquals("hello", Files.readString(outputPath));
    }
}

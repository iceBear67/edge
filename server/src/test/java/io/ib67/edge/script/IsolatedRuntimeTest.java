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

package io.ib67.edge.script;

import com.google.common.jimfs.Jimfs;
import io.ib67.edge.script.locator.DirectoryModuleLocator;
import lombok.SneakyThrows;
import org.graalvm.polyglot.*;
import org.graalvm.polyglot.io.IOAccess;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

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
                let Files = java.nio.file.Files;
                Files.writeString(outputPath, "hello");
                """;
        // let's validate that privileged code _actually_ works
        {
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
        }

        // then write it to the library
        var libraryCode = "export function writePrivileged(outputPath){\n%s\n}".formatted(privilegedCode);
        var libPath = libraryRoot.resolve("the_lib");
        Files.createDirectories(libPath);
        Files.writeString(libPath.resolve("index.mjs"), libraryCode);

        // create runtime which will scan libraries from that folder
        var runtime = new IsolatedRuntime(engine, new DirectoryModuleLocator(libraryRoot), HostAccess.newBuilder()
                .allowMapAccess(true) /* Map access to scope object is required */
                .build());

        // ... and the "privileged code" should NOT work in isolated runtime
        {
            var context = runtime.create(Source.create("js", ""), it -> it, it -> {
            });
            context.getScriptContext().getBindings("js").putMember("outputPath", libPath);
            assertThrows(PolyglotException.class, () -> context.eval(Source.create("js", privilegedCode)));
            context.close();

        }
        var callLibrarySource = Source.newBuilder("js", """
                var Java = Java;
                import { writePrivileged } from "@the_lib/index.mjs";
                export function test(){writePrivileged(outputPath)}
                """, "test.mjs").build();
        var context = runtime.create(callLibrarySource, it -> it, it -> it.putMember("outputPath", outputPath));
        context.getExportedMembers().get("test").executeVoid();
        context.close();
        assertTrue(Files.exists(outputPath));
        assertEquals("hello", Files.readString(outputPath));
    }
}

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
import io.ib67.edge.script.locator.ModuleLocator;
import lombok.SneakyThrows;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.*;

import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class LibraryStubTest {
    static FileSystem jimfs;
    static Path libRoot;
    static Path lib1Root;
    static Path lib2Root;
    Path stubCacheDir;
    Context context;

    @BeforeAll
    @SneakyThrows
    public static void setup() {
        jimfs = Jimfs.newFileSystem();
        var mockLibraryRoot = jimfs.getPath("/mock/path/");
        lib1Root = mockLibraryRoot.resolve("lib1");
        lib2Root = mockLibraryRoot.resolve("lib2");
        Files.createDirectories(mockLibraryRoot);
        Files.createDirectories(lib1Root);
        Files.createDirectories(lib2Root);
        Files.writeString(lib1Root.resolve("_index.mjs"), """
                let a = 1;
                export { a as aNumber}
                """);
        Files.writeString(lib1Root.resolve("normal.mjs"), """
                let b = 1;
                export { b as bNumber}
                """);
        Files.writeString(lib2Root.resolve("_index2.mjs"), """
                let a = "string";
                export { a as aString}
                """);
        libRoot = mockLibraryRoot;
    }

    @BeforeEach
    @SneakyThrows
    void setupContext() {
        stubCacheDir = jimfs.getPath("/stubcache/" + System.currentTimeMillis());
        Files.createDirectories(stubCacheDir);
        context = Context.newBuilder()
                .option("js.esm-eval-returns-exports", "true")
                .allowHostAccess(HostAccess.ALL)
                .allowHostClassLookup(it -> true)
                .build();
    }

    @AfterEach
    void teardown() {
        context.close(true);
    }

    @AfterAll
    @SneakyThrows
    static void globalTeardown() {
        jimfs.close();
    }

    @Test
    public void testCacheDiscoveredLibraries() {
        ModuleLocator locator = new DirectoryModuleLocator(libRoot);
        IsolatedRuntime.LibraryStubCache cache = new IsolatedRuntime.LibraryStubCache(context, locator);

        assertEquals(Set.of("lib1", "lib2"), cache.discoveredLibraries());
    }

    @Test
    public void testCacheGetSymbolsFromModule() {
        ModuleLocator locator = new DirectoryModuleLocator(libRoot);
        IsolatedRuntime.LibraryStubCache cache = new IsolatedRuntime.LibraryStubCache(context, locator);

        assertThrows(IllegalArgumentException.class, () -> cache.getSymbolsFromModule("nonexistent"));
        var lib1Sym = cache.getSymbolsFromModule("lib1");
        assertEquals(2, lib1Sym.size());
        assertEquals("aNumber", lib1Sym.getFirst());
        assertEquals("bNumber", lib1Sym.get(1));
        var lib2Sym = cache.getSymbolsFromModule("lib2");
        assertEquals(1, lib2Sym.size());
        assertEquals("aString", lib2Sym.getFirst());
    }

    @Test
    public void testCacheLibrarySources() {
        ModuleLocator locator = new DirectoryModuleLocator(libRoot);
        IsolatedRuntime.LibraryStubCache cache = new IsolatedRuntime.LibraryStubCache(context, locator);

        assertNotNull(cache.getLibrarySources());
        assertTrue(cache.getLibrarySources().containsKey("lib1"));
    }

    /// /////////////////////////////////////////////////////////////////////
    /// TESTS FOR CACHE LOCATOR
    /// /////////////////////////////////////////////////////////////////////

    @Test
    @SneakyThrows
    public void testLocatorLocateModule() {
        ModuleLocator mockLocator = new DirectoryModuleLocator(libRoot);
        IsolatedRuntime.LibraryStubCache mockCache = new IsolatedRuntime.LibraryStubCache(context, mockLocator);
        IsolatedRuntime.StubModuleLocator locator = new IsolatedRuntime.StubModuleLocator(stubCacheDir);
        locator.updateCache(mockCache);
        var binding = context.getBindings("js");
        var scope = Map.of(
                "lib1", Map.of(
                        "aNumber", 1,
                        "bNumber", 2
                ),
                "lib2", Map.of(
                        "aString", "str"
                )
        );
        binding.putMember("scope", scope);
        var evalExports = getExportedSymbols(locator, "lib1", "_index.mjs");
        assertEquals(1, evalExports.getMember("aNumber").asInt());
        assertEquals(2, evalExports.getMember("bNumber").asInt());

        evalExports = getExportedSymbols(locator, "lib2", "_index.mjs");
        assertEquals("str", evalExports.getMember("aString").asString());
    }

    @Test
    @SneakyThrows
    public void testLocatorUpdateCache() {
        ModuleLocator mockLocator = new DirectoryModuleLocator(libRoot);
        IsolatedRuntime.LibraryStubCache mockCache = new IsolatedRuntime.LibraryStubCache(context, mockLocator);
        IsolatedRuntime.StubModuleLocator locator = new IsolatedRuntime.StubModuleLocator(stubCacheDir);
        locator.updateCache(mockCache);
        var binding = context.getBindings("js");
        var scope = Map.of(
                "lib1", Map.of(
                        "aNumber", 1,
                        "bNumber", 2
                ),
                "lib2", Map.of(
                        "aString", "str"
                )
        );
        binding.putMember("scope", scope);
        assertNotNull(locator.getLastCache());
        var evalExports = getExportedSymbols(locator, "lib1", "_index.mjs");
        assertTrue(evalExports.hasMember("aNumber"));
        assertTrue(evalExports.hasMember("bNumber"));
        assertFalse(evalExports.hasMember("hello"));

        var newScript = lib1Root.resolve("additional.mjs");
        Files.writeString(newScript, """
                export function hello(){}
                """);
        locator.updateCache(new IsolatedRuntime.LibraryStubCache(context, mockLocator));
        evalExports = getExportedSymbols(locator, "lib1", "additional.mjs");
        assertTrue(evalExports.hasMember("aNumber"));
        assertTrue(evalExports.hasMember("bNumber"));
        assertTrue(evalExports.hasMember("hello"));
        Files.deleteIfExists(newScript);
    }

    @SneakyThrows
    Value getExportedSymbols(ModuleLocator locator, String module, String fileName) {
        Path result = locator.locateModule(module, fileName);
        var stubCode = Files.readString(result);
        System.out.println(stubCode);
        var stubSource = Source.newBuilder("js", stubCode, fileName).build();
        return context.eval(stubSource);
    }


    @Test
    public void testLocatorDiscoverLibraries() {
        ModuleLocator mockLocator = new DirectoryModuleLocator(libRoot);
        IsolatedRuntime.LibraryStubCache mockCache = new IsolatedRuntime.LibraryStubCache(context, mockLocator);
        IsolatedRuntime.StubModuleLocator locator = new IsolatedRuntime.StubModuleLocator(Paths.get("/tmp"));
        locator.updateCache(mockCache);

        assertEquals(Set.of("lib1", "lib2"), locator.discoverModules());
    }
}

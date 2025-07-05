package io.ib67.edge.script;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import io.ib67.edge.script.locator.DirectoryModuleLocator;
import io.ib67.edge.script.locator.ModuleLocator;
import io.ib67.edge.test.InfiniteMap;
import lombok.SneakyThrows;
import org.junit.jupiter.api.*;
import org.graalvm.polyglot.*;

import java.nio.file.*;
import java.util.*;

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
        ModuleRuntime.LibraryStubCache cache = new ModuleRuntime.LibraryStubCache(context, locator);

        assertEquals(Set.of("lib1", "lib2"), cache.discoveredLibraries());
    }

    @Test
    public void testCacheGetSymbolsFromModule() {
        ModuleLocator locator = new DirectoryModuleLocator(libRoot);
        ModuleRuntime.LibraryStubCache cache = new ModuleRuntime.LibraryStubCache(context, locator);

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
        ModuleRuntime.LibraryStubCache cache = new ModuleRuntime.LibraryStubCache(context, locator);

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
        ModuleRuntime.LibraryStubCache mockCache = new ModuleRuntime.LibraryStubCache(context, mockLocator);
        ModuleRuntime.StubModuleLocator locator = new ModuleRuntime.StubModuleLocator(stubCacheDir);
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
        ModuleRuntime.LibraryStubCache mockCache = new ModuleRuntime.LibraryStubCache(context, mockLocator);
        ModuleRuntime.StubModuleLocator locator = new ModuleRuntime.StubModuleLocator(stubCacheDir);
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
        locator.updateCache(new ModuleRuntime.LibraryStubCache(context, mockLocator));
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
        ModuleRuntime.LibraryStubCache mockCache = new ModuleRuntime.LibraryStubCache(context, mockLocator);
        ModuleRuntime.StubModuleLocator locator = new ModuleRuntime.StubModuleLocator(Paths.get("/tmp"));
        locator.updateCache(mockCache);

        assertEquals(Set.of("lib1", "lib2"), locator.discoverModules());
    }
}

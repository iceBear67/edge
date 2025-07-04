package io.ib67.edge.script;

import com.google.common.jimfs.Jimfs;
import io.ib67.edge.script.context.IncrementalModuleContext;
import io.ib67.edge.script.io.ESModuleFS;
import io.ib67.edge.script.locator.LibraryLocator;
import lombok.Getter;
import lombok.SneakyThrows;
import org.graalvm.polyglot.*;
import org.graalvm.polyglot.io.FileSystem;
import org.graalvm.polyglot.io.IOAccess;

import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import static java.lang.invoke.MethodHandles.lookup;

public class IsolatedRuntime extends ScriptRuntime {
    public record TrustedLibraryContext(
            IncrementalModuleContext scriptContext,
            StubLibraryLocator stubLocator
    ) implements AutoCloseable {
        // todo uniformed thread local management (lifecycle) to avoid memory leak
        @Override
        public void close() throws Exception {
            scriptContext.close();
        }
    }

    //todo write to host CACHE_DIR only when imfs is taking too many resources.
//    protected static final String CACHE_DIR =
//            System.getProperty("edge.isolatedruntime.stub.cachedir", System.getProperty("java.io.tmpdir"));
    protected static final String CACHE_DIR = "/_edge_cache";
    protected final ThreadLocal<TrustedLibraryContext> perThreadPrivileged;
    protected final LibraryLocator libraryLocator;
    protected final FileSystem fs;
    protected final java.nio.file.FileSystem cacheInMemFs;

    public IsolatedRuntime(Engine engine, java.nio.file.FileSystem nioFs, LibraryLocator locator) {
        super(engine);
        this.libraryLocator = locator;
        this.perThreadPrivileged = ThreadLocal.withInitial(this::createPrivilegeContext);
        this.cacheInMemFs = Jimfs.newFileSystem();
        var cacheGraalFS = FileSystem.newReadOnlyFileSystem(FileSystem.newFileSystem(cacheInMemFs));
        this.fs = FileSystem.newCompositeFileSystem(
                FileSystem.newFileSystem(nioFs),
                FileSystem.Selector.of(
                        cacheGraalFS, it -> it.normalize().startsWith(CACHE_DIR)
                )
        );
    }

    protected LibraryStubCache createStubCache(LibraryLocator locator) {
        var temporyContext = Context.newBuilder()
                .engine(this.engine)
                .option("js.esm-eval-returns-exports", "true")
                .allowHostAccess(HostAccess.ALL)
                .allowHostClassLookup(it -> true)
                .build();
        var stubCache = new LibraryStubCache(temporyContext, locator);
        temporyContext.close(true);
        return stubCache;
    }

    /**
     * This run on the ThreadLocal of context thread
     * @return
     */
    //todo hot-reload?
    protected TrustedLibraryContext createPrivilegeContext() {
        var context = Context
                .newBuilder()
                .engine(this.engine)
                .allowHostAccess(HostAccess.newBuilder().useModuleLookup(lookup()).build())
                .build();

        //todo versioned libraries based on jimfs or wtf
        var stubLocator = new StubLibraryLocator(cacheInMemFs.getPath(CACHE_DIR));
        var stubCache = createStubCache(this.libraryLocator);
        stubLocator.updateCache(stubCache);

        var scriptContext = new IncrementalModuleContext(context);
        var librarySources = stubCache.getLibrarySources();
        for (String module : librarySources.keySet()) {
            for (Source source : librarySources.get(module).source()) {
                scriptContext.evalModule(module, source);
            }
        }
        return new TrustedLibraryContext(scriptContext, stubLocator);
    }

    /**
     * Once a ScriptContext from IsolatedRuntime is initialized, they can ONLY run on THAT thread.
     * todo fix by providing a map delegate using threadlocal. This may also help versioned library hot reloading
     * @param binding
     */
    @Override
    protected void initializeBinding(Value binding) {
        super.initializeBinding(binding);
        var privilegedContext = perThreadPrivileged.get();
        var moduleExports = privilegedContext.scriptContext().getModuleExports();
        binding.putMember("scope", moduleExports);
    }

    @Override
    protected UnaryOperator<Context.Builder> configureContext() {
        return it -> it.allowIO(IOAccess.newBuilder()
                .allowHostSocketAccess(false)
                .fileSystem(FileSystem.newReadOnlyFileSystem(
                        new ESModuleFS(this.fs, () -> perThreadPrivileged.get().stubLocator())
                )).build());
    }

    protected static class StubLibraryLocator implements LibraryLocator {
        protected final Path cacheRoot;
        protected LibraryStubCache stubCache;
        protected Set<String> generatedStubs;

        protected StubLibraryLocator(
                Path cacheRoot
        ) {
            this.cacheRoot = cacheRoot;
        }

        public void updateCache(LibraryStubCache cache) {
            this.stubCache = cache;
            this.generatedStubs = new HashSet<>();
        }

        public LibraryStubCache getLastCache() {
            Objects.requireNonNull(stubCache, "stubCache not initialized.");
            return stubCache;
        }

        @Override
        public Set<String> discoverLibraries() {
            Objects.requireNonNull(stubCache, "stubCache not initialized.");
            return stubCache.discoveredLibraries();
        }

        protected static String generateStub(String scope, List<String> symbols) {
            var stub = new StringBuilder();
            for (String s : symbols) {
                stub.append("let _").append(s).append("=scope[\"").append(scope).append("\"].").append(s).append(";");
            }
            stub.append("\nexport {");
            stub.append(symbols.stream().map(it -> "_" + it + " as " + it).collect(Collectors.joining(",")));
            stub.append("}");
            return stub.toString();
        }

        @Override
        public Path locateRoot(String module) {
            var moduleRoot = cacheRoot.resolve(module);
            if (!moduleRoot.normalize().startsWith(cacheRoot.normalize()))
                throw new IllegalArgumentException("Module root is out of cache root: " + moduleRoot);
            return moduleRoot;
        }

        @Override
        @SneakyThrows
        public Path locateModule(String module, String file) {
            Objects.requireNonNull(stubCache, "stubCache not initialized.");
            var index = locateRoot(module).resolve("_index.mjs");
            if (generatedStubs.contains(module)) {
                return index;
            }
            Files.createDirectories(index.getParent());
            Files.writeString(index, generateStub(module, stubCache.getSymbolsFromModule(module)));
            generatedStubs.add(module);
            return index;
        }
    }

    protected static class LibraryStubCache {
        protected final LibraryLocator locator;
        @Getter
        protected final Map<String, Library> librarySources = new HashMap<>();
        protected final Map<String, List<String>> exportedSymbols = new HashMap<>();

        protected LibraryStubCache(Context cachePreloadContext, LibraryLocator locator) {
            this.locator = locator;
            buildCache(cachePreloadContext, locator);
        }

        private void buildCache(Context cachePreloadContext, LibraryLocator locator) {
            var libraries = locator.discoverLibraries();
            for (String library : libraries) {
                var libRoot = locator.locateRoot(library);
                librarySources.put(library, new Library(discoverSources(libRoot)));
            }
            for (String key : librarySources.keySet()) {
                exportedSymbols.put(key, librarySources.get(key).extractExportedSymbols(cachePreloadContext));
            }
        }

        public Set<String> discoveredLibraries() {
            return librarySources.keySet();
        }

        public List<String> getSymbolsFromModule(String module) {
            if (!exportedSymbols.containsKey(module)) {
                throw new IllegalArgumentException("Module " + module + " does not exist or does not export anything");
            }
            return exportedSymbols.get(module);
        }

        @SneakyThrows
        private List<Source> discoverSources(Path libRoot) {
            try (var stream = Files.walk(libRoot, 32, FileVisitOption.FOLLOW_LINKS)) {
                return stream.filter(Files::isRegularFile)
                        .filter(it -> it.getFileName().toString().endsWith(".mjs"))
                        .map(this::makeSourceOf)
                        .toList();
            }
        }

        @SneakyThrows
        private Source makeSourceOf(Path path) {
            return Source.newBuilder("js", Files.readString(path), path.getFileName().toString()).build();
        }

        protected record Library(List<Source> source) {
            public List<String> extractExportedSymbols(Context context) {
                var symbols = new ArrayList<String>();
                for (Source src : source) {
                    symbols.addAll(context.eval(src).getMemberKeys());
                }
                return symbols;
            }
        }

    }
}

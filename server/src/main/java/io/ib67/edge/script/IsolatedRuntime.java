package io.ib67.edge.script;

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

import static java.lang.invoke.MethodHandles.lookup;

public class IsolatedRuntime extends ScriptRuntime {
    public record TrustedLibraryContext(
            IncrementalModuleContext scriptContext,
            StubLibraryLocator stubLocator
    ) implements AutoCloseable {
        @Override
        public void close() throws Exception {
            scriptContext.close();
        }
    }

    protected static final String CACHE_DIR =
            System.getProperty("edge.isolatedruntime.stub.cachedir", System.getProperty("java.io.tmpdir"));
    // todo uniformed thread local management (lifecycle) to avoid memory leak
    protected final ThreadLocal<TrustedLibraryContext> perThreadPrivileged;
    protected final LibraryLocator libraryLocator;
    protected final FileSystem fs;

    public IsolatedRuntime(Engine engine, java.nio.file.FileSystem nioFs, LibraryLocator locator) {
        super(engine);
        this.libraryLocator = locator;
        this.perThreadPrivileged = ThreadLocal.withInitial(this::createPrivilegeContext);
        this.fs = FileSystem.newFileSystem(nioFs);
    }

    protected LibraryStubCache createStubCache(LibraryLocator locator) {
        var temporyContext = Context.newBuilder().engine(this.engine).build();
        var stubCache = new LibraryStubCache(temporyContext, locator);
        temporyContext.close(true);
        return stubCache;
    }

    //todo hot-reload?
    protected TrustedLibraryContext createPrivilegeContext() {
        var context = Context
                .newBuilder()
                .engine(this.engine)
                .allowHostAccess(HostAccess.newBuilder().useModuleLookup(lookup()).build())
                .build();

        var stubLocator = new StubLibraryLocator(Path.of(CACHE_DIR)); //todo versioned solution
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
                .fileSystem(new ESModuleFS(this.fs, () -> perThreadPrivileged.get().stubLocator()))
                .build());
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
                stub.append("let _").append(s).append("=scope[").append(scope).append("].").append(s).append(";");
            }
            stub.append("\nexport {\n");
            for (String s : symbols) {
                stub.append("    _").append(s).append(" as ").append(s).append("\n");
            }
            stub.append("}");
            return stub.toString();
        }

        @Override
        public Path locateRoot(String module) {
            var moduleRoot = cacheRoot.resolve(module);
            if (!moduleRoot.toAbsolutePath().startsWith(cacheRoot.toAbsolutePath()))
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
            Files.writeString(index, generateStub(module, stubCache.getExportedByModule(module)));
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

        public List<String> getExportedByModule(String module) {
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

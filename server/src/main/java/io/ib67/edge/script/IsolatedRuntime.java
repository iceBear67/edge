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
import io.ib67.edge.script.context.IncrementalModuleContext;
import io.ib67.edge.script.io.ESModuleFS;
import io.ib67.edge.script.locator.ModuleLocator;
import lombok.Getter;
import lombok.Setter;
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

public class IsolatedRuntime extends ScriptRuntime {
    public record ModuleContext(
            IncrementalModuleContext scriptContext,
            StubModuleLocator stubLocator
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
    protected final ThreadLocal<ModuleContext> perThreadLibrary;
    protected final ModuleLocator moduleLocator;
    protected final FileSystem guestFS;
    protected final Engine trustedEngine;
    protected final java.nio.file.FileSystem cacheInMemFs;
    @Getter
    protected final HostAccess hostAccess;
    @Getter
    @Setter
    protected Map<String, String> hostContextOptions = new HashMap<>();
    @Getter
    @Setter
    protected Map<String, String> guestContextOptions = new HashMap<>();

    public IsolatedRuntime(ModuleLocator moduleLocator) {
        this(Engine.create(),  moduleLocator, HostAccess.NONE);
    }

    public static HostAccess.Builder hostContainerAccess() {
        return HostAccess.newBuilder()
                .allowMapAccess(true)
                .allowListAccess(true)
                .allowArrayAccess(true)
                .allowIterableAccess(true)
                .allowIteratorAccess(true);
    }

    public IsolatedRuntime(
            Engine engine,
            ModuleLocator locator,
            HostAccess defaultAccess
    ) {
        super(engine);
        this.trustedEngine = Engine.create();
        // map access is necessary for guest codes to access libraries (in scope[])
        this.hostAccess = HostAccess.newBuilder(defaultAccess).allowMapAccess(true).build();
        this.moduleLocator = locator;
        this.perThreadLibrary = ThreadLocal.withInitial(this::createModuleContext);
        this.cacheInMemFs = Jimfs.newFileSystem();
        this.guestFS = FileSystem.newReadOnlyFileSystem(FileSystem.newFileSystem(cacheInMemFs));
    }

    protected Context.Builder createPrivilegedContext() {
        return Context.newBuilder()
                .options(hostContextOptions)
                .option("js.esm-eval-returns-exports", "true")
                .allowHostClassLookup(any -> true)
                .allowHostAccess(HostAccess.ALL)
                .engine(trustedEngine);
    }

    protected LibraryStubCache createStubCache(ModuleLocator locator) {
        var temporyContext = createPrivilegedContext().build();
        var stubCache = new LibraryStubCache(temporyContext, locator);
        temporyContext.close(true);
        return stubCache;
    }

    /**
     * This run on the ThreadLocal of context thread
     */
    //todo hot-reload?
    protected ModuleContext createModuleContext() {
        var context = createPrivilegedContext();

        //todo versioned libraries based on jimfs or wtf
        var stubLocator = new StubModuleLocator(cacheInMemFs.getPath(CACHE_DIR));
        var stubCache = createStubCache(this.moduleLocator);
        stubLocator.updateCache(stubCache);

        var scriptContext = new IncrementalModuleContext(context.build());
        var librarySources = stubCache.getLibrarySources();
        for (String module : librarySources.keySet()) {
            for (Source source : librarySources.get(module).source()) {
                scriptContext.evalModule(module, source);
            }
        }
        return new ModuleContext(scriptContext, stubLocator);
    }

    @Override
    protected void initializeBinding(Value binding) {
        super.initializeBinding(binding);
        var scope = new LocalModuleMap(perThreadLibrary);
        binding.putMember("scope", scope);
    }

    @Override
    protected UnaryOperator<Context.Builder> configureContext() {
        return it -> it
                .options(guestContextOptions)
                .option("js.esm-eval-returns-exports", "true")
                .allowHostAccess(getHostAccess())
                .allowIO(IOAccess.newBuilder()
                        .allowHostSocketAccess(false)
                        .fileSystem(FileSystem.newReadOnlyFileSystem(
                                new ESModuleFS(this.guestFS, () -> perThreadLibrary.get().stubLocator())
                        )).build());
    }

    protected static class StubModuleLocator implements ModuleLocator {
        protected final Path cacheRoot;
        protected LibraryStubCache stubCache;
        protected Set<String> generatedStubs;

        protected StubModuleLocator(
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
        public Set<String> discoverModules() {
            Objects.requireNonNull(stubCache, "stubCache not initialized.");
            return stubCache.discoveredLibraries();
        }

        protected static String generateStub(String scope, List<String> symbols) {
            var stub = new StringBuilder();
            for (String s : symbols) {
                stub.append("let _").append(s).append("=scope[\"").append(scope).append("\"][\"").append(s).append("\"];");
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
        protected final ModuleLocator locator;
        @Getter
        protected final Map<String, Library> librarySources = new HashMap<>();
        protected final Map<String, List<String>> exportedSymbols = new HashMap<>();

        protected LibraryStubCache(Context cachePreloadContext, ModuleLocator locator) {
            this.locator = locator;
            buildCache(cachePreloadContext, locator);
        }

        private void buildCache(Context cachePreloadContext, ModuleLocator locator) {
            var libraries = locator.discoverModules();
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

    @SuppressWarnings("unchecked")
    public static class LocalModuleMap extends AbstractMap<String, Object> {
        protected final ThreadLocal<ModuleContext> contextThreadLocal;

        public LocalModuleMap(ThreadLocal<ModuleContext> contextThreadLocal) {
            this.contextThreadLocal = contextThreadLocal;
        }

        protected IncrementalModuleContext getScriptContext() {
            return contextThreadLocal.get().scriptContext();
        }

        @Override
        public int size() {
            return getScriptContext().getModuleExports().size();
        }

        @Override
        public boolean isEmpty() {
            return getScriptContext().getModuleExports().isEmpty();
        }

        @Override
        public boolean containsKey(Object key) {
            return getScriptContext().getModuleExports().containsKey(key);
        }

        @Override
        public boolean containsValue(Object value) {
            return getScriptContext().getModuleExports().containsValue(value);
        }

        @HostAccess.Export
        @Override
        public Object get(Object key) {
            return getScriptContext().getModuleExports().get(key);
        }

        @Override
        public Object put(String key, Object value) {
            throw new UnsupportedOperationException("scope is readonly to guest code");
        }

        @Override
        public Object remove(Object key) {
            throw new UnsupportedOperationException("scope is readonly to guest code");
        }

        @Override
        public void putAll(Map<? extends String, ?> m) {
            throw new UnsupportedOperationException("scope is readonly to guest code");
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException("scope is readonly to guest code");
        }

        @Override
        @HostAccess.Export
        public Set<String> keySet() {
            return getScriptContext().getModuleExports().keySet();
        }

        @Override
        @HostAccess.Export
        public Collection<Object> values() {
            return (Collection<Object>) (Object) getScriptContext().getModuleExports().values();
        }

        @Override
        public Set<Entry<String, Object>> entrySet() {
            return (Set<Entry<String, Object>>) (Object) getScriptContext().getModuleExports().entrySet();
        }
    }
}

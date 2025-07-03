package io.ib67.edge.script;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.io.FileSystem;
import org.graalvm.polyglot.io.IOAccess;

import java.util.Collection;
import java.util.Objects;
import java.util.function.UnaryOperator;

public class ContextOptionParser {
    protected final FileSystem hostFs;
    protected final FileSystem readOnlyHostFs;

    public ContextOptionParser(java.nio.file.FileSystem hostFs) {
        this.hostFs = FileSystem.newFileSystem(hostFs);
        this.readOnlyHostFs = FileSystem.newReadOnlyFileSystem(this.hostFs);
    }

    public UnaryOperator<Context.Builder> parse(Collection<? extends ScriptOption> options) {
        Objects.requireNonNull(options);
        return context -> {
            var fsSelectors = options.stream()
                    .filter(it -> it instanceof ScriptOption.PathAccess)
                    .map(ScriptOption.PathAccess.class::cast)
                    .map(this::makeSelector).toList();
            context.allowIO(IOAccess.newBuilder()
                    .allowHostSocketAccess(false)
                    .fileSystem(
                            FileSystem.newCompositeFileSystem(
                                    FileSystem.newDenyIOFileSystem(), fsSelectors.toArray(new FileSystem.Selector[0]))
                    ).build());
            options.stream().filter(it -> it instanceof ScriptOption.ContextOption)
                    .forEach(it-> context.option(((ScriptOption.ContextOption) it).key(), ((ScriptOption.ContextOption) it).value()));
            return context;
        };
    }

    private FileSystem.Selector makeSelector(ScriptOption.PathAccess pathAccess) {
        if (pathAccess.readOnly()) {
            return FileSystem.Selector.of(FileSystem.newReadOnlyFileSystem(hostFs), path -> path.startsWith(pathAccess.prefix()));
        }
        return FileSystem.Selector.of(hostFs, path -> path.startsWith(pathAccess.prefix()));
    }
}

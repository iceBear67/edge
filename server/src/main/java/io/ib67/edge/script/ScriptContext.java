package io.ib67.edge.script;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
public class ScriptContext implements AutoCloseable {
    @Getter
    protected final Context scriptContext;
    protected final List<Runnable> cleanupHandlers;

    public ScriptContext(Context scriptContext) {
        this.scriptContext = scriptContext;
        this.cleanupHandlers = new ArrayList<>();
        initializeBindings(scriptContext);
    }

    protected void initializeBindings(Context scriptContext) {
        var binding = scriptContext.getPolyglotBindings();
        binding.putMember("onClean", (Consumer<Runnable>) this::script$onClean);
    }

    public void addCleanup(Runnable runnable) {
        this.cleanupHandlers.add(runnable);
    }

    public Value eval(Source source) {
        return scriptContext.eval(source);
    }

    protected void script$onClean(Runnable runnable) {
        cleanupHandlers.add(runnable);
    }

    @Override
    public void close() throws IOException {
        for (Runnable managedResource : cleanupHandlers) {
            try {
                managedResource.run();
            } catch (Exception e) {
                log.error("Error calling cleanup handler", e);
            }
        }
    }
}

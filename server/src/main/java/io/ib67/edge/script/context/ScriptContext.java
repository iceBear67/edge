package io.ib67.edge.script.context;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import java.io.IOException;
import java.util.*;
import java.util.function.BiConsumer;

@Slf4j
public class ScriptContext implements AutoCloseable {
    @Getter
    protected final Context scriptContext;
    protected final Map<String, Value> exportedMembers = new HashMap<>();
    protected final Map<String, List<Runnable>> lifecycleHandlers;
    protected final Source source;

    public ScriptContext(Context scriptContext, Source entrypoint) {
        this.scriptContext = scriptContext;
        this.source = entrypoint;
        this.lifecycleHandlers = new HashMap<>();
        initializeBindings(scriptContext.getBindings(entrypoint.getLanguage()));

        var bindings = this.scriptContext.getBindings(entrypoint.getLanguage());
        var exports = scriptContext.eval(this.source);
        for (String memberKey : exports.getMemberKeys()) {
            exportedMembers.put(memberKey, bindings.getMember(memberKey));
        }
    }

    protected void initializeBindings(Value binding) {
        binding.putMember("onLifecycle", (BiConsumer<String, Runnable>) this::script$registerHandler);
    }


    public Value eval(Source source) {
        return scriptContext.eval(source);
    }

    protected void script$registerHandler(String event, Runnable runnable) {
        lifecycleHandlers.computeIfAbsent(event, k -> new ArrayList<>()).add(runnable);
    }

    public void onLifecycleEvent(String event) {
        for (Runnable managedResource : lifecycleHandlers.getOrDefault(event, List.of())) {
            try {
                managedResource.run();
            } catch (Exception e) {
                log.error("Error calling cleanup handler", e);
            }
        }
    }

    public Map<String, Value> getExportedMembers() {
        return Collections.unmodifiableMap(exportedMembers);
    }

    @Override
    public void close() throws IOException {
        onLifecycleEvent("clean");
        scriptContext.close(true);
    }
}

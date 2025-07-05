package io.ib67.edge.script.context;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import java.io.IOException;
import java.util.*;

@Slf4j
public class ScriptContext implements AutoCloseable {
    @Getter
    protected final Context scriptContext;
    protected final Map<String, Value> exportedMembers = new HashMap<>();
    protected final Map<String, List<Runnable>> lifecycleHandlers;
    protected final Source source;

    //todo regulate scriptContext must have js.esm-eval-returns-exports or factory
    //todo this constructor should not be public.
    protected ScriptContext(Context scriptContext, Source entrypoint) {
        this.scriptContext = scriptContext;
        this.source = entrypoint;
        this.lifecycleHandlers = new HashMap<>();
        try {
            initializeBindings(scriptContext.getBindings(entrypoint.getLanguage()));
            var exports = scriptContext.eval(this.source);
            if (exports != null) {
                for (String memberKey : exports.getMemberKeys()) {
                    exportedMembers.put(memberKey, exports.getMember(memberKey));
                }
            }
        } catch (RuntimeException e) { //todo maybe a checked exception
            System.err.println(e.getClass().getCanonicalName());
            e.printStackTrace();
            scriptContext.close(true);
            throw e;
        }
    }

    protected void initializeBindings(Value binding) {
        binding.putMember("context", new ScriptInterface());
    }


    public Value eval(Source source) {
        return scriptContext.eval(source);
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

    public class ScriptInterface {
        @HostAccess.Export
        public void on(String event, Value runnable) {
            var handlers = lifecycleHandlers.computeIfAbsent(event, k -> new ArrayList<>());
            if (runnable.canExecute()) {
                handlers.add(runnable::executeVoid);
                return;
            }
            throw new IllegalArgumentException("Value " + runnable + " is not executable");
        }
    }
}

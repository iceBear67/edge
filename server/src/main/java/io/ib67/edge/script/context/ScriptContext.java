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

package io.ib67.edge.script.context;

import io.ib67.edge.script.exception.ContextInitException;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import java.io.IOException;
import java.util.*;

@Log4j2
public class ScriptContext implements AutoCloseable {
    @Getter
    private volatile boolean initialized = false;
    @Getter
    protected final Context scriptContext;
    protected final Map<String, Value> exportedMembers = new HashMap<>();
    protected final Map<String, List<Runnable>> lifecycleHandlers;
    @Getter
    protected final Source source;
    private final Value parsedSource;

    protected ScriptContext(Context scriptContext, Source entrypoint) {
        this.scriptContext = scriptContext;
        this.source = entrypoint;
        this.lifecycleHandlers = new HashMap<>();
        this.parsedSource = scriptContext.parse(entrypoint);
    }

    public void init() throws ContextInitException {
        if (initialized) throw new IllegalStateException("Script context already initialized");
        try {
            initializeBindings(scriptContext.getBindings(source.getLanguage()));
            populateExportedModules(parsedSource);
        } catch (RuntimeException e) {
            scriptContext.close(true);
            throw new ContextInitException(e);
        }
        initialized = true;
    }

    protected void populateExportedModules(Value parsedSource) {
        var exports = parsedSource.execute();
        if (exports != null) {
            for (String memberKey : exports.getMemberKeys()) {
                exportedMembers.put(memberKey, exports.getMember(memberKey));
            }
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

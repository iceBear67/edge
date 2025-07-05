package io.ib67.edge.script.context;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class IncrementalModuleContext extends ScriptContext {
    protected static final Source EMPTY = Source.create("js", "");
    protected final Map<String, Value> moduleExports = new HashMap<>();

    public IncrementalModuleContext(Context scriptContext) {
        super(scriptContext, EMPTY);
    }

    public Map<String, Value> getModuleExports() {
        return Collections.unmodifiableMap(moduleExports);
    }

    @Override
    public Map<String, Value> getExportedMembers() {
        return Collections.unmodifiableMap(moduleExports);
    }

    public Value evalModule(String module, Source source) {
        var result = eval(source);
        if (moduleExports.containsKey(module)) {
            var existing = moduleExports.get(module);
            for (String memberKey : result.getMemberKeys()) {
                existing.putMember(memberKey, result.getMember(memberKey));
            }
        } else {
            moduleExports.put(module, result);
        }
        return result;
    }
}

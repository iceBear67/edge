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

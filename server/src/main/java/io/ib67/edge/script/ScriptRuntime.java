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

import io.ib67.edge.script.context.ScriptContext;
import io.ib67.edge.script.exception.ContextInitException;
import org.graalvm.polyglot.*;
import org.graalvm.polyglot.io.IOAccess;

import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public class ScriptRuntime {
    protected final Engine engine;

    public ScriptRuntime(Engine engine) {
        this.engine = engine;
    }

    protected void initializeBinding(Value binding) {
    }

    protected UnaryOperator<Context.Builder> configureContext() {
        return it -> it.allowIO(IOAccess.NONE).allowHostAccess(getHostAccess());
    }

    protected HostAccess getHostAccess() {
        return HostAccess.NONE;
    }

    public ScriptContext create(Source source) throws ContextInitException {
        return create(source, it -> {
        });
    }

    public ScriptContext create(
            Source source,
            Consumer<Value> bindingOperator
    ) {
        var _context = Context.newBuilder().engine(engine);
        _context = configureContext().apply(_context).allowHostAccess(getHostAccess());
        var gContext = _context.build();
        return new ScriptContext(gContext, source) {
            @Override
            protected void initializeBindings(Value binding) {
                super.initializeBindings(binding);
                initializeBinding(binding);
                bindingOperator.accept(binding);
            }
        };
    }
}

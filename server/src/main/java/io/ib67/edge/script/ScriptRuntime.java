package io.ib67.edge.script;

import io.ib67.edge.script.context.ScriptContext;
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

    public ScriptContext create(Source source, UnaryOperator<Context.Builder> operator){
        return create(source, operator, it->{});
    }

    public ScriptContext create(
            Source source,
            UnaryOperator<Context.Builder> operator, //todo consider removal
            Consumer<Value> bindingOperator
    ) {
        var _context = Context.newBuilder().engine(engine);
        _context = configureContext().apply(operator.apply(_context)).allowHostAccess(getHostAccess());
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

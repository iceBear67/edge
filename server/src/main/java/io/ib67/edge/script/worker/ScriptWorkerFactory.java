package io.ib67.edge.script.worker;

import io.ib67.edge.Deployment;
import io.ib67.edge.api.RequestHandler;
import io.ib67.edge.script.ContextOptionParser;
import io.ib67.edge.script.ScriptContext;
import io.ib67.edge.script.ScriptOption;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotAccess;

import java.nio.file.FileSystems;

public class ScriptWorkerFactory {
    public ScriptWorker create(Deployment deployment) {
        var _context = Context.newBuilder();
        _context = new ContextOptionParser(FileSystems.getDefault()).parse(deployment.options()).apply(_context);
        var context = _context.allowHostAccess(HostAccess.ALL).allowHostClassLookup(it->true).build();
        return new ScriptWorker(new ScriptContext(context), ()->{}, deployment.source());
    }
}

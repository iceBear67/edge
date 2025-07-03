package io.ib67.edge.script.worker;

import io.ib67.edge.api.RequestHandler;
import io.ib67.edge.script.ScriptContext;
import io.ib67.edge.serializer.HttpRequestBox;
import io.ib67.edge.worker.Worker;
import io.vertx.core.eventbus.Message;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;

/**
 * ScriptWorker 将请求转发给关联起来的脚本。脚本需要符合 es6 规范或者返回 export {} object
 */
public class ScriptWorker extends Worker {
    protected final ScriptContext context;
    protected final Source source;
    protected final RequestHandler handler;

    public ScriptWorker(ScriptContext context, Runnable onClose, Source source) {
        super(onClose);
        this.context = context;
        this.source = source;
        var exportedFunctions = context.eval(source);
        handler = exportedFunctions.getMember("handleRequest").as(RequestHandler.class);
    }

    @Override
    public void start() {
        super.start();
    }

    @Override
    protected void handleRequest0(Message<HttpRequestBox> objectMessage) {
        var req = objectMessage.body().request();
        try {
            handler.handleRequest(req);
        } catch (Exception throwable) {
            req.response().setStatusCode(500);
            req.response().end("Error: " + throwable.getMessage());
        }
    }
}

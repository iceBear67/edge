package io.ib67.edge.worker;

import io.ib67.edge.api.RequestHandler;
import io.ib67.edge.script.context.ScriptContext;
import io.ib67.edge.serializer.HttpRequestBox;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;

/**
 * ScriptWorker 将请求转发给关联起来的脚本。脚本需要符合 es6 规范或者返回 export {} object
 */
public class ScriptWorker extends Worker {
    protected final ScriptContext context;
    protected final RequestHandler handler;

    public ScriptWorker(ScriptContext context, Runnable onClose) {
        super(onClose);
        this.context = context;
        handler = context.getExportedMembers().get("handleRequest").as(RequestHandler.class);
    }

    @Override
    public void start() {
        context.onLifecycleEvent("start");
        super.start();
    }

    @Override
    public void stop(Promise<Void> stopPromise) throws Exception {
        context.onLifecycleEvent("stop");
        super.stop(stopPromise);
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

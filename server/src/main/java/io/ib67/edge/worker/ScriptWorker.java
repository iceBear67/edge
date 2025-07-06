package io.ib67.edge.worker;

import io.ib67.edge.api.RequestHandler;
import io.ib67.edge.script.context.ScriptContext;
import io.ib67.edge.serializer.HttpRequestBox;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import lombok.extern.log4j.Log4j2;

/**
 * ScriptWorker 将请求转发给关联起来的脚本。脚本需要符合 esm 规范返回 export {}
 */
@Log4j2
public class ScriptWorker extends Worker {
    protected final ScriptContext context;
    protected final RequestHandler handler;

    public ScriptWorker(ScriptContext context, Runnable onClose) {
        super(onClose);
        this.context = context;
        var binding = context.getScriptContext().getBindings("js");
        binding.putMember("log", log);
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
            log.error("Error handling request {}/{}", req.getHeader("Host"), req.path(), throwable);
        }
    }
}

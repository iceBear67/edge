package io.ib67.edge;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.ib67.edge.script.ContextOptionParser;
import io.ib67.edge.script.ScriptRuntime;
import io.ib67.edge.serializer.AnyMessageCodec;
import io.ib67.edge.serializer.HttpRequestBox;
import io.ib67.edge.worker.ScriptWorker;
import io.ib67.edge.worker.Worker;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerRequest;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.FileSystems;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class ServerVerticle extends AbstractVerticle {
    protected final Map<String, Worker> workers = new HashMap<>();
    protected final ObjectMapper mapper = new ObjectMapper();
    protected final ScriptRuntime runtime;

    public ServerVerticle(ScriptRuntime contextFactory) {
        this.runtime = contextFactory;
    }

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        getVertx().eventBus().registerDefaultCodec(HttpRequestBox.class, new AnyMessageCodec<>(HttpRequestBox.class));
        getVertx().createHttpServer()
                .requestHandler(this::onRequest)
                .listen(8080)
                .onComplete(it -> startPromise.complete());
    }

    public void deploy(Deployment deployment) {
        var scriptContext = runtime.create(deployment.source(), new ContextOptionParser(FileSystems.getDefault()).parse(deployment.options()));
        var worker = new ScriptWorker(scriptContext, () ->
                log.info("ScriptWorker " + deployment.name() + " v" + deployment.version() + " is shutting down..."));
        workers.put(deployment.name().toLowerCase(), worker);
        vertx.deployVerticle(worker);
    }

    private void onRequest(HttpServerRequest httpServerRequest) {
        var host = httpServerRequest.getHeader("Host");
        if (host == null || host.isEmpty()) {
            httpServerRequest.end();
            return;
        }
        var firstDot = host.indexOf(".");
        if (firstDot == -1) {
            httpServerRequest.response().setStatusCode(400);
            httpServerRequest.end();
            return;
        }
        var prefix = host.substring(0, firstDot);
        var worker = workers.get(prefix.toLowerCase());
        if (worker != null) {
            worker.handleRequest(getVertx(), httpServerRequest);
        } else {
            httpServerRequest.response().setStatusCode(404);
            httpServerRequest.response().end();
        }

    }
}

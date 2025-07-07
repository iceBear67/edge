package io.ib67.edge;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.ib67.edge.script.ScriptRuntime;
import io.ib67.edge.serializer.AnyMessageCodec;
import io.ib67.edge.serializer.HttpRequestBox;
import io.ib67.edge.worker.ScriptWorker;
import io.ib67.edge.worker.Worker;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerRequest;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

@Log4j2
public class ServerVerticle extends AbstractVerticle {
    protected final Map<String, Worker> workers = new HashMap<>();
    protected final ObjectMapper mapper = new ObjectMapper();
    @Getter
    protected final String host;
    @Getter
    protected final int port;
    protected final ScriptRuntime runtime;

    public ServerVerticle(String host, int port, ScriptRuntime runtime) {
        this.host = host;
        this.runtime = runtime;
        this.port = port;
    }

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        getVertx().eventBus().registerDefaultCodec(HttpRequestBox.class, new AnyMessageCodec<>(HttpRequestBox.class));
        getVertx().createHttpServer()
                .requestHandler(this::onRequest)
                .listen(port, host)
                .onComplete(it -> {
                    log.info("Server verticle {} is listening on {}:{}", this.deploymentID(), host, port);
                    startPromise.complete();
                });
    }

    public void deploy(Deployment deployment) {
        log.info("Deploying : {}", deployment.toHumanReadable());
        var scriptContext = runtime.create(deployment.source(), UnaryOperator.identity());
        var worker = new ScriptWorker(scriptContext, () ->
                log.info("ScriptWorker {} is shutting down...", deployment.toHumanReadable()));
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

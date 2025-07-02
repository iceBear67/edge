package io.ib67.edge;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.ib67.edge.script.worker.ScriptWorkerFactory;
import io.ib67.edge.serializer.AnyMessageCodec;
import io.ib67.edge.serializer.HttpRequestBox;
import io.ib67.edge.worker.Worker;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import lombok.SneakyThrows;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ServerVerticle extends AbstractVerticle {
    protected final Map<String, Worker> workers = new HashMap<>();
    protected final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        var router = Router.router(vertx);
        router.post("/deploy").handler(this::onPostDeploy);
        var promiseA = Promise.promise();
        var promiseB = Promise.promise();
        getVertx().eventBus().registerDefaultCodec(HttpRequestBox.class, new AnyMessageCodec<>(HttpRequestBox.class));
        getVertx().createHttpServer()
                .requestHandler(this::onRequest)
                .listen(8080)
                .onComplete(it -> promiseA.complete());
        getVertx().createHttpServer()
                .requestHandler(router)
                .listen(8081)
                .onComplete(it -> promiseB.complete());
        Future.join((Future<?>) promiseA, (Future<?>) promiseB).onComplete(it -> startPromise.future());
    }

    @SneakyThrows
    private void onPostDeploy(RoutingContext routingContext) {
        routingContext.request().bodyHandler(buffer -> {
            try {
                var deployment = mapper.readValue(buffer.getBytes(), Deployment.class);
                deploy(deployment);
                routingContext.response().end();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void deploy(Deployment deployment) {
        var worker = new ScriptWorkerFactory().create(deployment);
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
        if(worker != null) {
            worker.handleRequest(getVertx(), httpServerRequest);
        }else{
            httpServerRequest.response().setStatusCode(404);
            httpServerRequest.response().end();
        }

    }
}

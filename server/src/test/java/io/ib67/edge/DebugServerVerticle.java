package io.ib67.edge;

import io.ib67.edge.script.IsolatedRuntime;
import io.ib67.edge.script.ScriptRuntime;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Log4j2
public class DebugServerVerticle extends ServerVerticle {
    public DebugServerVerticle(ScriptRuntime engine) {
        super("localhost", 8080, engine);
    }

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        var promiseA = Promise.<Void>promise();
        var promiseB = Promise.promise();
        super.start(promiseA);
        var router = Router.router(vertx);
        router.post("/deploy").handler(this::onPostDeploy);
        getVertx().createHttpServer()
                .requestHandler(router)
                .listen(8081)
                .onComplete(it -> promiseB.complete());
        Future.join((Future<?>) promiseA, (Future<?>) promiseB).onComplete(it -> startPromise.complete());
    }

    @SneakyThrows
    private void onPostDeploy(RoutingContext routingContext) {
        log.info("Processing a new deployment request");
        routingContext.request().bodyHandler(buffer -> {
            try {
                var deployment = mapper.readValue(buffer.getBytes(), Deployment.class);
                log.info("Deploying " + deployment.name() + " ver" + deployment.version());
                deploy(deployment);
                routingContext.response().end();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

}

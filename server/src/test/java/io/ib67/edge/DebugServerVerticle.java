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

package io.ib67.edge;

import io.ib67.edge.script.ScriptRuntime;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

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

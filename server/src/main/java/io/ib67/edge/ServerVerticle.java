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
import io.ib67.edge.serializer.AnyMessageCodec;
import io.ib67.edge.serializer.HttpRequestBox;
import io.ib67.edge.worker.ScriptWorker;
import io.ib67.kiwi.routine.Result;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerRequest;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ServerVerticle extends AbstractVerticle {
    protected final WorkerRouter workerRouter = new WorkerRouter(vertx);
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
                    log.info("Server verticle {} is listening on {}:{} ", this.deploymentID(), host, port);
                    startPromise.complete();
                });
    }


    @SneakyThrows
    public void deploy(Deployment deployment) {
        workerRouter.registerWorker(
                deployment.name(),
                () -> Result.fromAny(() -> runtime.create(deployment.source()))
                        .map(scriptContext -> new ScriptWorker(scriptContext, () -> log.info("ScriptWorker {} is shutting down...", deployment.name())))
                        .orElseThrow()
        ).onFailure(err -> log.error("Cannot deploy worker for deployment {}", deployment.name(), err));
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
        var worker = workerRouter.getWorker(prefix);
        if (worker != null) {
            worker.handleRequest(getVertx(), httpServerRequest);
        } else {
            httpServerRequest.response().setStatusCode(404);
            httpServerRequest.response().end();
        }

    }
}

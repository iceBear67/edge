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

import io.ib67.edge.api.EdgeServer;
import io.ib67.edge.api.event.AsyncWorkerContextEvent;
import io.ib67.edge.api.event.ComponentInitEvent;
import io.ib67.edge.api.event.PreRequestEvent;
import io.ib67.edge.config.ServerConfig;
import io.ib67.edge.script.ScriptRuntime;
import io.ib67.edge.serializer.AnyMessageCodec;
import io.ib67.edge.serializer.HttpRequestBox;
import io.ib67.edge.worker.ScriptWorker;
import io.ib67.edge.worker.Worker;
import io.ib67.kiwi.event.api.EventBus;
import io.ib67.kiwi.routine.Result;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.HostAndPort;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.graalvm.polyglot.Value;

import java.util.HashMap;
import java.util.Map;

@Log4j2
public class ServerVerticle extends AbstractVerticle implements EdgeServer {
    @Getter
    protected final String host;
    @Getter
    protected final int port;
    @Getter
    protected final ScriptRuntime runtime;
    protected final EventBus eventBus;
    protected WorkerRouter workerRouter;

    public ServerVerticle(ServerConfig config, ScriptRuntime runtime, EventBus eventBus) {
        this.host = config.listenHost();
        this.runtime = runtime;
        this.port = config.listenPort();
        this.eventBus = eventBus;
    }

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        getVertx().eventBus().registerDefaultCodec(HttpRequestBox.class, new AnyMessageCodec<>(HttpRequestBox.class));
        workerRouter = new WorkerRouter(getVertx());
        getVertx().createHttpServer()
                .requestHandler(this::onRequest)
                .listen(port, host)
                .onComplete(it -> {
                    log.info("Server verticle {} is listening on {}:{} ", this.deploymentID(), host, port);
                    startPromise.complete();
                });
        eventBus.post(new ComponentInitEvent<>(this, EdgeServer.class));
    }


    @SneakyThrows
    public Future<String> deploy(Deployment deployment) {
        return workerRouter.registerWorker(
                deployment.name(),
                () -> Result.fromAny(() -> runtime.create(deployment.source(), v -> injectDependencies(v, deployment)))
                        .map(scriptContext -> new ScriptWorker(scriptContext, deployment, () -> log.info("ScriptWorker {} is shutting down...", deployment.name())))
                        .orElseThrow()
        ).onFailure(err -> log.error("Cannot deploy worker for deployment {}", deployment.name(), err));
    }

    private void injectDependencies(Value value, Deployment deployment) {
        var env = new HashMap<>(deployment.env());
        env.put("RUNTIME", "edge");
        env.put("DEPLOY_SINCE", String.valueOf(System.currentTimeMillis()));
        env.put("DEPLOY_NAME", deployment.name());
        var plugins = new HashMap<String, Object>();
        var event = new AsyncWorkerContextEvent(env, plugins, value, deployment);
        eventBus.post(event);
        value.putMember("env", env);
        value.putMember("plugins", plugins);
    }

    private void onRequest(HttpServerRequest httpServerRequest) {
        var host = HostAndPort.authority(httpServerRequest.getHeader("Host")).host();
        var event = new PreRequestEvent(httpServerRequest);
        eventBus.post(event);
        if (event.isIntercepted()) return;
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
        event.setWorker(worker);
        eventBus.post(event);
        worker = event.getWorker();
        if (worker != null) {
            worker.handleRequest(getVertx(), httpServerRequest);
        } else {
            httpServerRequest.response().setStatusCode(404);
            httpServerRequest.response().end();
        }
    }

    @Override
    public Map<String, Worker> getWorkers() {
        return workerRouter.getWorkers();
    }
}

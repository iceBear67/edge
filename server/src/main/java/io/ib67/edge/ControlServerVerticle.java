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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Getter
public class ControlServerVerticle extends AbstractVerticle {
    protected final ServerVerticle serverVerticle;
    @Getter(AccessLevel.PRIVATE)
    protected final ObjectMapper mapper;
    protected final String host;
    protected final int port;

    public ControlServerVerticle(String host, int port, ServerVerticle serverVerticle) {
        this.serverVerticle = serverVerticle;
        this.mapper = JsonMapper.builder()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .build();
        this.host = host;
        this.port = port;
    }

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        var router = Router.router(vertx);
        router.post("/deploy").handler(this::onPostDeploy);
        getVertx().createHttpServer()
                .requestHandler(router)
                .listen(port, host)
                .onComplete(it -> {
                    log.info("Control server has been started.");
                    startPromise.complete();
                });
    }

    @SneakyThrows
    private void onPostDeploy(RoutingContext routingContext) {
        log.info("Processing a new deployment request");
        routingContext.request().bodyHandler(buffer -> {
            try {
                var deployment = mapper.readValue(buffer.getBytes(), Deployment.class);
                log.info("Deploying {}", deployment.toHumanReadable());
                serverVerticle.deploy(deployment);
                routingContext.response().end();
            } catch (Exception e) {
                routingContext.fail(e);
            }
        });
    }
}

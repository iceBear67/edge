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

package io.ib67.edge.init;

import com.google.inject.AbstractModule;
import io.ib67.edge.ControlServerVerticle;
import io.ib67.edge.ServerVerticle;
import io.ib67.edge.api.EdgeServer;
import io.ib67.edge.api.script.ExportToScript;
import io.ib67.edge.api.script.future.Thenable;
import io.ib67.edge.config.ServerConfig;
import io.ib67.edge.script.IsolatedRuntime;
import io.ib67.edge.script.ScriptRuntime;
import io.ib67.edge.script.locator.DirectoryModuleLocator;
import io.ib67.kiwi.event.api.EventBus;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.graalvm.polyglot.Engine;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Log4j2
@RequiredArgsConstructor
public class MainModule extends AbstractModule {
    private final EventBus bus;
    private final ServerConfig serverConfig;
    private final Vertx vertx;

    @Override
    @SneakyThrows
    protected void configure() {
        super.configure();
        var runtime = initializeRuntime(serverConfig);
        bind(EventBus.class).toInstance(bus);
        bind(Vertx.class).toInstance(vertx);
        bind(ScriptRuntime.class).toInstance(runtime);
        bind(ServerConfig.class).toInstance(serverConfig);

        log.info("Deploying server verticle...");
        var serverVerticle = new ServerVerticle(serverConfig, runtime, bus);
        bind(EdgeServer.class).toInstance(serverVerticle);
        vertx.deployVerticle(serverVerticle);

        if (serverConfig.controlListenPort() > 0) {
            log.info("Deploying control server...");
            var controlServerVerticle = new ControlServerVerticle(
                    serverConfig.controlListenHost(),
                    serverConfig.controlListenPort(),
                    serverVerticle
            );
            vertx.deployVerticle(controlServerVerticle);
        } else {
            log.info("Control server has been disabled.");
        }
    }

    private @NotNull IsolatedRuntime initializeRuntime(ServerConfig serverConfig) throws IOException {
        log.info("Initializing Graal Polyglot Engine");
        var engine = Engine.newBuilder("js")
                .in(InputStream.nullInputStream())
                .err(OutputStream.nullOutputStream())
                .out(OutputStream.nullOutputStream())
                .options(serverConfig.engineOptions())
                .build();
        assert Future.succeededFuture() instanceof Thenable : "Mixin is not working yet";

        var pathLibraries = Path.of(serverConfig.runtime().pathLibraries());
        Files.createDirectories(pathLibraries);
        log.info("Initializing runtime...");
        var runtime = new IsolatedRuntime(
                engine,
                new DirectoryModuleLocator(pathLibraries),
                IsolatedRuntime.hostContainerAccess()
                        .allowImplementationsAnnotatedBy(ExportToScript.class)
                        .allowAccessAnnotatedBy(ExportToScript.class)
                        .build()
        );
        runtime.setHostContextOptions(serverConfig.runtime().hostContextOptions());
        runtime.setGuestContextOptions(serverConfig.runtime().guestContextOptions());
        return runtime;
    }
}

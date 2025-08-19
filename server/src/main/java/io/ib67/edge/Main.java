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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.ib67.edge.api.EdgePlugin;
import io.ib67.edge.api.event.LifecycleEvents;
import io.ib67.edge.api.event.init.EdgeServerInitializedEvent;
import io.ib67.edge.api.event.init.RuntimeInitializedEvent;
import io.ib67.edge.api.script.ExportToScript;
import io.ib67.edge.api.script.future.Thenable;
import io.ib67.edge.command.CommandLoop;
import io.ib67.edge.command.StopCommand;
import io.ib67.edge.config.ServerConfig;
import io.ib67.edge.plugin.PersistDeploymentPlugin;
import io.ib67.edge.script.IsolatedRuntime;
import io.ib67.edge.script.locator.DirectoryModuleLocator;
import io.ib67.kiwi.event.HierarchyEventBus;
import io.ib67.kiwi.event.api.EventBus;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.graalvm.polyglot.Engine;
import org.pf4j.DefaultPluginManager;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Log4j2
public class Main {
    private static final String CONFIG_PATH = System.getProperty("edge.config", "config.json");
    private static final Map<String, Supplier<PersistDeploymentPlugin>> BUILT_IN_PLUGINS;

    static {
        BUILT_IN_PLUGINS = Map.of(
                "persist deployments", PersistDeploymentPlugin::new
        );
    }

    @SneakyThrows
    public static void main(String[] args) {
        var begin = System.currentTimeMillis();
        log.info("Initializing edge server...");
        var om = JsonMapper.builder()
                .configure(JsonParser.Feature.ALLOW_COMMENTS, true)
                .configure(JsonParser.Feature.IGNORE_UNDEFINED, true)
                .configure(SerializationFeature.INDENT_OUTPUT, true)
                .build();
        var configFile = new File(CONFIG_PATH);
        if (!configFile.exists()) {
            om.writeValue(configFile, ServerConfig.defaultConfig());
        }
        var serverConfig = om.readValue(configFile, ServerConfig.class);
        var vertx = Vertx.vertx();
        var bus = new HierarchyEventBus();
        loadPlugins(serverConfig, bus);
        var engine = Engine.newBuilder()
                .in(InputStream.nullInputStream())
                .out(OutputStream.nullOutputStream()) // todo logging management
                .options(serverConfig.engineOptions())
                .build();
        assert Future.succeededFuture() instanceof Thenable : "Mixin is not working yet";
        Files.createDirectories(Path.of(serverConfig.runtime().pathLibraries()));
        log.info("Initializing runtime...");
        var runtime = new IsolatedRuntime(
                engine,
                new DirectoryModuleLocator(Path.of(serverConfig.runtime().pathLibraries())),
                IsolatedRuntime.hostContainerAccess()
                        .allowImplementationsAnnotatedBy(ExportToScript.class)
                        .allowAccessAnnotatedBy(ExportToScript.class)
                        .build()
        );
        runtime.setHostContextOptions(serverConfig.runtime().hostContextOptions());
        runtime.setGuestContextOptions(serverConfig.runtime().guestContextOptions());
        bus.post(new RuntimeInitializedEvent(runtime));
        log.info("Deploying server verticle...");
        var serverVerticle = new ServerVerticle(serverConfig.listenHost(), serverConfig.listenPort(), runtime, bus);
        vertx.deployVerticle(serverVerticle);
        if (serverConfig.controlListenPort() < 0) {
            log.info("Control server has been disabled.");
            return;
        }
        bus.post(new EdgeServerInitializedEvent(serverVerticle));
        log.info("Deploying control server...");
        var controlServerVerticle = new ControlServerVerticle(
                serverConfig.controlListenHost(),
                serverConfig.controlListenPort(),
                serverVerticle
        );
        vertx.deployVerticle(controlServerVerticle);
        bus.post(LifecycleEvents.SERVER_START);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down server...");
            bus.post(LifecycleEvents.SERVER_STOP);
        }));
        log.info("Server started! ({}s)", (System.currentTimeMillis() - begin)/1000);
        initREPL();
    }

    private static void initREPL() {
        var commandLoop = new CommandLoop(
                new StopCommand()
        );
        commandLoop.run(System.in);
    }

    private static void loadPlugins(ServerConfig config, EventBus bus) {
        var pm = new DefaultPluginManager();
        pm.loadPlugins();
        pm.startPlugins();
        var builtIn = config.builtInPlugins();
        if(builtIn == null) builtIn = List.of();
        for (String builtInPlugin : builtIn) {
            var pluginSupplier = BUILT_IN_PLUGINS.get(builtInPlugin);
            if (pluginSupplier == null) {
                log.error("No built-in plugin named `{}` were found.", builtInPlugin);
                continue;
            }
            pluginSupplier.get().registerListener(bus);
        }
        var extensions = pm.getExtensions(EdgePlugin.class);
        for (EdgePlugin extension : extensions) {
            extension.registerListener(bus);
        }
    }
}

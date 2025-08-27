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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.ib67.edge.api.event.CommandInitEvent;
import io.ib67.edge.api.event.ComponentInitEvent;
import io.ib67.edge.api.event.LifecycleEvents;
import io.ib67.edge.api.plugin.EdgePlugin;
import io.ib67.edge.api.plugin.PluginConfig;
import io.ib67.edge.api.script.ExportToScript;
import io.ib67.edge.api.script.future.Thenable;
import io.ib67.edge.command.Command;
import io.ib67.edge.command.CommandLoop;
import io.ib67.edge.command.StopCommand;
import io.ib67.edge.config.AllPluginConfig;
import io.ib67.edge.config.ServerConfig;
import io.ib67.edge.plugin.PersistDeploymentPlugin;
import io.ib67.edge.script.IsolatedRuntime;
import io.ib67.edge.script.locator.DirectoryModuleLocator;
import io.ib67.kiwi.TypeToken;
import io.ib67.kiwi.event.HierarchyEventBus;
import io.ib67.kiwi.event.api.EventBus;
import io.ib67.kiwi.routine.Fail;
import io.ib67.kiwi.routine.Result;
import io.ib67.kiwi.routine.Some;
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
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Supplier;

@Log4j2
public class Main {
    private static final String CONFIG_PATH = System.getProperty("edge.config", "config.yml");
    private static final String PLUGIN_CONFIG_PATH = System.getProperty("edge.config.plugin", "plugin.yml");
    private static final Map<String, Supplier<EdgePlugin>> BUILT_IN_PLUGINS;

    static {
        BUILT_IN_PLUGINS = Map.of(
                "persistent deployments", PersistDeploymentPlugin::new
        );
    }

    @SneakyThrows
    public static void main(String[] args) {
        var begin = System.currentTimeMillis();
        log.info("Initializing edge server...");
        var om = YAMLMapper.builder()
                .configure(YAMLGenerator.Feature.WRITE_DOC_START_MARKER, false)
                .configure(SerializationFeature.INDENT_OUTPUT, true)
                .build();
        ;
        var configFile = new File(CONFIG_PATH);
        if (!configFile.exists()) {
            om.writeValue(configFile, ServerConfig.defaultConfig());
        }
        var serverConfig = om.readValue(configFile, ServerConfig.class);
        var vertx = Vertx.vertx();
        var bus = new HierarchyEventBus();
        loadPlugins(om, vertx, bus);
        var engine = Engine.newBuilder("js")
                .in(InputStream.nullInputStream())
                .err(OutputStream.nullOutputStream())
                .out(OutputStream.nullOutputStream()) // todo logging management
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
        bus.post(new ComponentInitEvent<>(runtime));
        log.info("Deploying server verticle...");
        var serverVerticle = new ServerVerticle(serverConfig.listenHost(), serverConfig.listenPort(), runtime, bus);
        vertx.deployVerticle(serverVerticle);
        if (serverConfig.controlListenPort() < 0) {
            log.info("Control server has been disabled.");
            return;
        }
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
        log.info("Server started! ({}s)", (System.currentTimeMillis() - begin) / 1000);
        var commands = new ArrayList<Command>();
        commands.add(new StopCommand());
        bus.post(new CommandInitEvent(commands::add));
        var commandLoop = new CommandLoop(
                commands.toArray(new Command[0])
        );
        initREPL(commandLoop);
    }

    private static void initREPL(CommandLoop loop) {
        loop.run(System.in);
    }

    @SneakyThrows
    private static void loadPlugins(
            ObjectMapper configMapper,
            Vertx vertx,
            EventBus bus
    ) {
        var pm = new DefaultPluginManager();
        pm.loadPlugins();
        pm.startPlugins();
        var pathToPluginConfig = Path.of(PLUGIN_CONFIG_PATH);
        AllPluginConfig allPluginConfig;
        if (Files.notExists(pathToPluginConfig)) {
            allPluginConfig = new AllPluginConfig();
        } else {
            allPluginConfig = configMapper.readValue(Files.readAllBytes(pathToPluginConfig), AllPluginConfig.class);
        }

        var enabledExtensions = new ArrayList<EdgePlugin>();
        BUILT_IN_PLUGINS.entrySet().stream()
                .filter(it -> allPluginConfig.enabledPlugins().contains(it.getKey()))
                .forEach(it -> enabledExtensions.add(it.getValue().get()));

        var extensions = pm.getExtensions(EdgePlugin.class);
        extensions.stream()
                .filter(it -> it.getName() != null && !it.getName().isBlank())
                .filter(it -> allPluginConfig.enabledPlugins().contains(it.getName()))
                .forEach(enabledExtensions::add);
        var superConfig = allPluginConfig.configs();
        var markForRecode = false;
        for (EdgePlugin extension : enabledExtensions) {
            var name = extension.getName();
            var type = TypeToken.resolve(extension.getClass()).inferType(EdgePlugin.class);
            if (type.isWildcard() || type.isArray()) {
                log.warn("{}: config type must not be array or wildcard.", name);
                continue;
            }
            var configType = type.getTypeParams().getFirst().getBaseTypeRaw();
            PluginConfig pluginConfig = null;
            if (configType != AllPluginConfig.class) { // type of config provided.
                var _config = superConfig.get(name);
                if (_config != null) {
                    if (configType != _config.getClass()) {
                        log.warn("Type of configuration for plugin {} is different from {}!", name, configType);
                        log.warn("Skipping this plugin.");
                        continue;
                    }
                    pluginConfig = _config;
                } else if (configType != null) {
                    var defaultConfig = switch (Result.fromAny(() -> configType.getConstructor().newInstance())) {
                        case Some(PluginConfig config) -> config;
                        case Fail(Object reason) -> {
                            log.warn("Failed to construct config for {}. Config classes must have a empty arg constructor and visible to eternal callers!", name);
                            log.warn("Error: {}", reason);
                            log.warn("Skipping this plugin.");
                            yield null;
                        }
                        default -> throw new AssertionError("as");
                    };
                    if (defaultConfig == null) continue;
                    superConfig.put(name, defaultConfig);
                    pluginConfig = defaultConfig;
                    markForRecode = true;
                }
            }
            extension.init(vertx, bus, pluginConfig);
        }
        if (markForRecode) {
            Files.write(pathToPluginConfig, configMapper.writeValueAsBytes(allPluginConfig));
        }
    }
}

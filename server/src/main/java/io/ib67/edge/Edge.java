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

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ib67.edge.api.event.CommandInitEvent;
import io.ib67.edge.api.event.ServerStopEvent;
import io.ib67.edge.command.Command;
import io.ib67.edge.command.CommandLoop;
import io.ib67.edge.command.StopCommand;
import io.ib67.edge.config.ServerConfig;
import io.ib67.edge.init.MainModule;
import io.ib67.edge.init.PluginInitModule;
import io.ib67.edge.init.PluginPreInitModule;
import io.ib67.edge.plugin.EdgePluginManager;
import io.ib67.kiwi.event.HierarchyEventBus;
import io.ib67.kiwi.event.api.EventBus;
import io.ib67.kiwi.routine.Result;
import io.ib67.kiwi.routine.Uni;
import io.vertx.core.Vertx;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

@Log4j2
public class Edge {
    private static final String CONFIG_PATH = System.getProperty("edge.config", "config.yml");
    private static final YAMLMapper CONFIG_MAPPER = YAMLMapper.builder()
            .configure(YAMLGenerator.Feature.WRITE_DOC_START_MARKER, false)
            .configure(SerializationFeature.INDENT_OUTPUT, true)
            .build();
    @Getter
    private static Injector defaultInjector;

    @SneakyThrows
    public static void main(String[] args) {
        var begin = System.currentTimeMillis();
        log.info("Initializing edge server...");
        var serverConfig = loadConfig();
        var bus = new HierarchyEventBus();
        var injector = Guice.createInjector(new MainModule(bus, serverConfig, Vertx.vertx()));
        var pm = new EdgePluginManager(CONFIG_MAPPER);
        pm.setExtensionInjector(injector);
        var pluginInjector = injector.createChildInjector(new PluginPreInitModule(CONFIG_MAPPER, pm), new PluginInitModule(pm));
        pm.setExtensionInjector(pluginInjector);
        Edge.defaultInjector = pluginInjector;
        Uni.from(pm.getEdgePlugins()::forEach)
                .map(it -> Result.runAny(it::init))
                .onItem(result ->
                        result.onFail(f ->
                                log.error("Failed to initialize plugin: {}", f.failure())
                        )
                );
        log.info("Server started! ({}s)", (System.currentTimeMillis() - begin) / 1000);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down server...");
            bus.post(ServerStopEvent.INSTANCE);
        }));
        initREPL(bus);
    }

    private static ServerConfig loadConfig() throws IOException {
        var om = CONFIG_MAPPER;
        var configFile = new File(CONFIG_PATH);
        if (!configFile.exists()) {
            om.writeValue(configFile, ServerConfig.defaultConfig());
        }
        return om.readValue(configFile, ServerConfig.class);
    }

    private static void initREPL(EventBus bus) {
        var commands = new ArrayList<Command>();
        commands.add(new StopCommand());
        bus.post(new CommandInitEvent(commands::add));
        var commandLoop = new CommandLoop(
                commands.toArray(new Command[0])
        );
        commandLoop.run(System.in);
    }
}

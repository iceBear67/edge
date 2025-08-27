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

package io.ib67.edge.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.ib67.edge.Deployment;
import io.ib67.edge.api.EdgeServer;
import io.ib67.edge.api.event.ComponentInitEvent;
import io.ib67.edge.api.event.LifecycleEvents;
import io.ib67.edge.api.plugin.EdgePlugin;
import io.ib67.edge.api.plugin.PluginConfig;
import io.ib67.edge.worker.ScriptWorker;
import io.ib67.kiwi.event.api.EventBus;
import io.ib67.kiwi.event.api.EventListenerHost;
import io.ib67.kiwi.event.api.annotation.SubscribeEvent;
import io.vertx.core.Vertx;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import java.nio.file.Files;
import java.nio.file.Path;

@Log4j2
public class PersistDeploymentPlugin implements EdgePlugin<PersistDeploymentPlugin.PersistRulesConfig>, EventListenerHost {
    protected EdgeServer server;
    protected final ObjectMapper mapper = new ObjectMapper();
    protected Path path;

    @Override
    public String getName() {
        return "persist deployments";
    }

    @Override
    public void init(Vertx vertx, EventBus bus, PersistRulesConfig config) {
        assert config != null;
        path = Path.of(config.savePath());
        this.registerTo(bus);
    }

    @SubscribeEvent
    @SneakyThrows
    void handleServerInitialized(ComponentInitEvent<EdgeServer> edgeServerInitializedEvent) {
        this.server = edgeServerInitializedEvent.component();
        log.info("Locating previously saved deployments...");
        if (Files.notExists(path)) {
            log.info("No deployments are found.");
            return;
        }
        try (var stream = Files.walk(path)) {
            stream
                    .filter(Files::isRegularFile)
                    .peek(it -> log.info("Loading {}", it))
                    .map(this::tryLoadDeployment).forEach(it -> server.deploy(it));
        }
    }

    @SneakyThrows
    private Deployment tryLoadDeployment(Path path) {
        return mapper.readValue(Files.readAllBytes(path), Deployment.class);
    }

    @SneakyThrows
    @SubscribeEvent
    void handleSaveDeployments(LifecycleEvents event) {
        if (event != LifecycleEvents.SERVER_STOP || server == null) return;
        log.info("Saving deployments");
        var workers = server.getWorkers();
        if (Files.notExists(path)) {
            Files.createDirectory(path);
        }
        for (var entry : workers.entrySet()) {
            var name = entry.getKey();
            var value = entry.getValue();
            if (!(value instanceof ScriptWorker worker)) continue;
            var save = path.resolve(name + ".json");
            Files.write(save, mapper.writeValueAsBytes(worker.getDeployment()));
            log.info("Saved deployment: {}", name);
        }
        log.info("All deployments saved without exception!");
    }

    public record PersistRulesConfig(
            String savePath
    ) implements PluginConfig {
        public PersistRulesConfig() {
            this("saved_deployments");
        }
    }
}

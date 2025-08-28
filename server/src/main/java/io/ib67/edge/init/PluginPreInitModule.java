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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import io.ib67.edge.api.plugin.EdgePlugin;
import io.ib67.edge.api.plugin.EdgePluginConfig;
import io.ib67.edge.api.plugin.PluginConfig;
import io.ib67.edge.plugin.EdgePluginManager;
import io.ib67.kiwi.TypeToken;
import io.ib67.kiwi.routine.Fail;
import io.ib67.kiwi.routine.Result;
import io.ib67.kiwi.routine.Some;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import java.nio.file.Files;
import java.nio.file.Path;

@RequiredArgsConstructor
@Log4j2
public class PluginPreInitModule extends AbstractModule {
    private static final String PLUGIN_CONFIG_PATH = System.getProperty("edge.config", "plugin-configs/");
    protected final ObjectMapper configMapper;
    protected final EdgePluginManager pm;

    @SneakyThrows
    @Override
    protected void configure() {
        var configRoot = Path.of(PLUGIN_CONFIG_PATH);
        if (Files.notExists(configRoot)) {
            Files.createDirectory(configRoot);
        }
        bind(EdgePluginManager.class).toInstance(pm);
        var extensions = pm.getExtensionClasses(EdgePlugin.class);
        for (Class<? extends EdgePlugin> extension : extensions) {
            var typeToken = TypeToken.resolve(extension).inferType(EdgePlugin.class)
                    .getTypeParams().getFirst();
            if (typeToken.isArray() || typeToken.isWildcard()) {
                log.error("Plugin config cannot be array or wildcard type.");
                continue;
            }
            var configType = typeToken.getBaseTypeRaw();
            if (configType == PluginConfig.class) continue; // custom config type does not provided.
            var anno = configType.getAnnotation(EdgePluginConfig.class);
            Path pluginConfigPath;
            if (anno == null) {
                pluginConfigPath = configRoot.resolve(extension.getSimpleName().toLowerCase() + ".yml");
                log.warn("@EdgePluginConfig is not annotated on the config {}", configType);
                log.warn("The default name '{}' will be used.", pluginConfigPath.getFileName());
            } else {
                pluginConfigPath = configRoot.resolve(anno.value());
            }
            if(Files.exists(pluginConfigPath)) {
                var config = configMapper.readValue(pluginConfigPath.toFile(), configType);
                bind((Class<PluginConfig>) configType).toInstance((PluginConfig) config);
            }else {
                log.warn("Creating default config for {}...", extension.getCanonicalName());
                switch (Result.fromAny(() -> typeToken.getBaseTypeRaw().getConstructor().newInstance())) {
                    case Some(PluginConfig config) -> {
                        configMapper.writeValue(pluginConfigPath.toFile(), config);
                        bind((Class<PluginConfig>) config.getClass()).toInstance(config);
                    }
                    case Fail(Throwable throwable) -> log.error(
                            "Cannot save default config for {}, error: {}",
                            extension.getCanonicalName(), throwable.getMessage()
                    );
                    default -> throw new AssertionError();
                }
            }
        }
    }
}

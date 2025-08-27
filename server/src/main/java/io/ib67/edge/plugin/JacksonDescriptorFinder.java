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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.pf4j.PluginDependency;
import org.pf4j.PluginDescriptor;
import org.pf4j.PluginDescriptorFinder;
import org.pf4j.PluginRuntimeException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipFile;

public class JacksonDescriptorFinder implements PluginDescriptorFinder {
    protected final ObjectMapper configMapper;
    protected final String descriptorPath;

    public JacksonDescriptorFinder(ObjectMapper configMapper, String descriptorPath) {
        this.configMapper = configMapper;
        this.descriptorPath = descriptorPath;
    }

    @Override
    public boolean isApplicable(Path pluginPath) {
        return Files.exists(pluginPath) && Files.isRegularFile(pluginPath)
                && (pluginPath.endsWith(".jar") || pluginPath.endsWith(".zip"));
    }

    @Override
    public PluginDescriptor find(Path pluginPath) {
        try (var zipFile = new ZipFile(pluginPath.toFile())) {
            var entry = zipFile.getEntry(descriptorPath);
            if (entry == null) throw new PluginRuntimeException("Unable to find plugin.yml");
            try (var is = zipFile.getInputStream(entry)) {
                return configMapper.readValue(is, SimplePluginDescriptor.class);
            }
        } catch (IOException e) {
            throw new PluginRuntimeException("Cannot open plugin", e);
        }
    }

    @RequiredArgsConstructor
    @Getter
    static final class SimplePluginDescriptor implements PluginDescriptor {
        @JsonProperty("id")
        private final String pluginId;
        @JsonProperty("description")
        private final String pluginDescription;
        @JsonProperty("main")
        private final String pluginClass;
        private final String version;
        private final String requires;
        private final String provider;
        private final String license;
        @JsonProperty("dependencies")
        @Getter(AccessLevel.PRIVATE)
        private final List<String> _dependencies;

        public List<PluginDependency> getDependencies() {
            return _dependencies.stream().map(PluginDependency::new).toList();
        }
    }
}

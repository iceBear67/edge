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
import io.ib67.edge.api.plugin.EdgePlugin;
import io.ib67.edge.config.ServerConfig;
import io.ib67.edge.plugin.EdgePluginManager;
import lombok.RequiredArgsConstructor;
import org.pf4j.PluginWrapper;

import java.util.HashMap;

@RequiredArgsConstructor
public class PluginInitModule extends AbstractModule {
    protected final EdgePluginManager pluginManager;
    protected final ServerConfig serverConfig;

    @Override
    protected void configure() {
        var associationMap = new HashMap<ClassLoader, PluginWrapper>();
        for (PluginWrapper plugin : pluginManager.getPlugins()) {
            associationMap.put(plugin.getPluginClassLoader(), plugin);
        }
        var enabledExtensions = pluginManager.getExtensions(EdgePlugin.class)
                .stream().filter(it ->
                        serverConfig.enabledPlugins().contains(
                                associationMap.get(it.getClass().getClassLoader()).getPluginId()))
                .toList();
        for (EdgePlugin extension : enabledExtensions) {
            pluginManager.addPlugin(extension);
            bind((Class<EdgePlugin>) extension.getClass()).toInstance(extension);
        }
    }
}

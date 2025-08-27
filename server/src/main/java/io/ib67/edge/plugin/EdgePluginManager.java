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
import com.google.inject.Injector;
import io.ib67.edge.api.plugin.EdgePlugin;
import io.ib67.edge.api.plugin.PluginRegistry;
import io.ib67.edge.config.ServerConfig;
import io.ib67.edge.init.PluginInitModule;
import io.ib67.edge.init.PluginPreInitModule;
import org.jetbrains.annotations.Nullable;
import org.pf4j.*;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class EdgePluginManager extends DefaultPluginManager implements PluginRegistry {
    protected final Map<Class<? extends EdgePlugin>, EdgePlugin> edgePlugins = new HashMap<>();
    protected final ObjectMapper configMapper;
    protected final GuiceExtensionFactory extensionFactory;
    protected final Injector parentInjector;
    protected Injector pluginInjector;

    public EdgePluginManager(Injector injector, ObjectMapper configMapper, Path... pluginsRoots) {
        super(pluginsRoots);
        this.parentInjector = injector;
        this.configMapper = configMapper;
        this.extensionFactory = new GuiceExtensionFactory(injector);
    }

    public Injector initPlugins() {
        var injector = parentInjector;
        var configInjector = injector.createChildInjector(new PluginPreInitModule(configMapper, this));
        extensionFactory.injector = configInjector;
        pluginInjector = configInjector.createChildInjector(
                new PluginInitModule(
                        configInjector.getInstance(EdgePluginManager.class),
                        injector.getInstance(ServerConfig.class)
                )
        );
        extensionFactory.injector = parentInjector;
        return pluginInjector;
    }

    @Override
    protected ExtensionFactory createExtensionFactory() {
        return extensionFactory;
    }

    @Override
    protected PluginDescriptorFinder createPluginDescriptorFinder() {
        return new JacksonDescriptorFinder(configMapper, "plugin.yml");
    }

    @Override
    protected ExtensionFinder createExtensionFinder() {
        DefaultExtensionFinder extensionFinder = new DefaultExtensionFinder(this);
        addPluginStateListener(extensionFinder);
        // for debugging convenience and internal plugins.
//        extensionFinder.addServiceProviderExtensionFinder();
        return extensionFinder;
    }

    @Override
    public @Nullable <E extends EdgePlugin<?>> E getEdgePlugin(Class<E> plugin) {
        return (E) edgePlugins.get(plugin);
    }

    @Override
    public Collection<? extends EdgePlugin> getEdgePlugins() {
        return Collections.unmodifiableCollection(edgePlugins.values());
    }

    public void addPlugin(EdgePlugin plugin) {
        edgePlugins.put(plugin.getClass(), plugin);
    }
}

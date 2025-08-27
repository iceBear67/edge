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
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.pf4j.*;

import java.nio.file.Path;
import java.util.*;

public class EdgePluginManager extends DefaultPluginManager implements PluginRegistry {
    protected final Map<Class<? extends EdgePlugin>, EdgePlugin> edgePlugins = new HashMap<>();
    protected final ObjectMapper configMapper;
    protected GuiceExtensionFactory extensionFactory;

    public EdgePluginManager(ObjectMapper configMapper, Path... pluginsRoots) {
        super(pluginsRoots);
        this.configMapper = configMapper;
    }

    @ApiStatus.Internal
    public void setExtensionInjector(Injector injector) {
        Objects.requireNonNull(injector);
        extensionFactory.injector = injector;
    }

    @Override
    protected ExtensionFactory createExtensionFactory() {
        this.extensionFactory = new GuiceExtensionFactory(null);
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

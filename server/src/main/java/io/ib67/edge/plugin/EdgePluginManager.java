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

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
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
    protected GuiceExtensionFactory extensionFactory;

    public EdgePluginManager(Path... pluginsRoots) {
        super(pluginsRoots);
    }

    @ApiStatus.Internal
    public void setExtensionInjector(Injector injector) {
        Objects.requireNonNull(injector);
        extensionFactory.injector = injector;
    }

    @Override
    public PluginWrapper getPlugin(String pluginId) {
        var pl = super.getPlugin(pluginId);
        // dirty hack for loading extensions without started plugins.
        // this behaviour is based on AbstaactExtensionFinder#find(type, id), which
        // requires plugin state is STARTED to load extensions.
        pl.setPluginState(PluginState.STARTED);
        return pl;
    }

    @Override
    protected ExtensionFactory createExtensionFactory() {
        this.extensionFactory = new GuiceExtensionFactory(null);
        return extensionFactory;
    }

    @Override
    protected PluginDescriptorFinder createPluginDescriptorFinder() {
        var mapper =  YAMLMapper.builder()
                .configure(YAMLGenerator.Feature.WRITE_DOC_START_MARKER, false)
                .configure(SerializationFeature.INDENT_OUTPUT, true)
                .build();
        return new JacksonDescriptorFinder(mapper, "plugin.yml");
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

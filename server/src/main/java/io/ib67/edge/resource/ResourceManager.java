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

package io.ib67.edge.resource;

import io.ib67.edge.api.script.ExportToScript;
import io.ib67.edge.resource.pool.SimpleResourcePool;
import io.ib67.edge.resource.pool.SynchronizedResourcePool;
import io.ib67.edge.resource.pool.ThreadLocalPool;
import io.vertx.core.Future;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ResourceManager {
    protected final Map<Class<?>, Resource<?>> resources = new ConcurrentHashMap<>();
    protected final Map<Resource<?>, ResourcePool<?>> pooledResources = new ConcurrentHashMap<>();

    @ExportToScript
    public <T> Future<Recyclable<T>> getResource(Class<T> type) {
        var resource = resources.get(type);
        if (resource == null) {
            return Future.failedFuture("Resource not found: " + type);
        }
        if (resource.threadMode() == Resource.ThreadMode.CREATE_ON_DEMAND) {
            return Future.succeededFuture(new Recyclable.NotRecyclable<>((T) resource.resourceFactory().get()));
        }
        return ((ResourcePool<T>) pooledResources.get(resource)).acquireResource();
    }

    public <T> void registerResource(Class<T> type, Resource<T> resource) {
        switch (resource.threadMode()) {
            case SHARED ->
                    pooledResources.put(resource,
                            new SynchronizedResourcePool<>(
                                    new SimpleResourcePool<>(resource, resource.poolSize())));
            case THREAD_LOCAL -> pooledResources.put(resource, new ThreadLocalPool<>(resource, resource.poolSize()));
            case CREATE_ON_DEMAND -> {
            }
        }
        resources.put(type, resource);
    }
}

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

package io.ib67.edge.resource.pool;

import io.ib67.edge.resource.Recyclable;
import io.ib67.edge.resource.Resource;
import io.ib67.edge.resource.ResourcePool;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.ArrayDeque;
import java.util.Deque;

public class SimpleResourcePool<T> implements ResourcePool<T> {
    protected final Deque<Promise<Recyclable<T>>> pendingFutures;
    protected final Resource<T> resource;
    protected final Deque<T> pool;
    protected final int poolSize;
    protected int totalResourceCreated;
    protected boolean closed;

    public SimpleResourcePool(Resource<T> resource, int poolSize) {
        this.resource = resource;
        this.poolSize = poolSize;
        pool = new ArrayDeque<>(poolSize);
        pendingFutures = new ArrayDeque<>();
    }

    @Override
    public Future<Recyclable<T>> acquireResource() {
        ensureNotClosed();
        if (pool.isEmpty()) {
            if (totalResourceCreated == poolSize) {
                Promise<Recyclable<T>> promise = Promise.promise();
                pendingFutures.add(promise);
                return promise.future();
            }
            totalResourceCreated++;
            return resource.resourceFactory().get()
                    .map(RecyclableImpl::new);
        }
        return Future.succeededFuture(pool.pollFirst()).map(RecyclableImpl::new);
    }

    void releaseResource(T resource) {
        if (closed) {
            this.resource.close().accept(resource);
            return;
        }
        if (!pendingFutures.isEmpty()) {
            pendingFutures.pollFirst().tryComplete(new RecyclableImpl(resource));
            return;
        }
        pool.push(resource);
    }

    void ensureNotClosed() {
        if (closed) throw new IllegalStateException("The pool is already closed");
    }

    @Override
    public void close() throws Exception {
        ensureNotClosed();
        closed = true;
        for (Promise<Recyclable<T>> pendingFuture : pendingFutures) {
            pendingFuture.fail("Pool is closing");
        }
        for (T t : pool) {
            resource.close().accept(t);
        }
        pool.clear();
    }

    @EqualsAndHashCode
    @ToString
    final class RecyclableImpl implements Recyclable<T> {
        private final T unwrap;
        private boolean recycled;

        RecyclableImpl(T unwrap) {
            this.unwrap = unwrap;
        }

        @Override
        public void recycle() {
            mustNotRecycled();
            recycled = true;
            releaseResource(unwrap);
        }

        @Override
        public T unwrap() {
            mustNotRecycled();
            return unwrap;
        }

        private void mustNotRecycled() {
            if (recycled) throw new IllegalStateException("resource " + unwrap + " is already recycled.");
        }
    }
}

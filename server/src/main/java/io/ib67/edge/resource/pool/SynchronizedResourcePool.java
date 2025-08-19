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
import io.ib67.edge.resource.ResourcePool;
import io.vertx.core.Future;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SynchronizedResourcePool<T> implements ResourcePool<T> {
    protected final ResourcePool<T> resourcePool;
    protected final Lock lock;
    protected volatile boolean closed;

    public SynchronizedResourcePool(ResourcePool<T> resourcePool) {
        this.resourcePool = resourcePool;
        this.lock = new ReentrantLock();
    }

    @Override
    public Future<Recyclable<T>> acquireResource() {
        if(closed) throw new IllegalStateException("pool is closed");
        lock.lock();
        try {
            return resourcePool.acquireResource().map(SynchronizedRecyclable::new);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() throws Exception {
        lock.lock();
        closed = true;
        lock.unlock();
        resourcePool.close();
    }

    final class SynchronizedRecyclable implements Recyclable<T> {
        private final Recyclable<T> r;
        private volatile boolean closed;

        SynchronizedRecyclable(Recyclable<T> r) {
            this.r = r;
        }

        @Override
        public T unwrap() {
            if (closed) throw new IllegalStateException("Resource is recycled");
            return r.unwrap();
        }

        @Override
        public void recycle() {
            closed = true;
            lock.lock();
            try {
                r.recycle();
            } finally {
                lock.unlock();
            }
        }
    }
}

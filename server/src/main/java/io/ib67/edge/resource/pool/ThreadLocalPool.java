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
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayDeque;
import java.util.Deque;

public class ThreadLocalPool<T> implements ResourcePool<T> {
    protected static final boolean DETECT_RECYCLABLE_THREAD_LEAK = Boolean.getBoolean("edge.detectRecyclableThreadLeak");
    protected final ThreadLocal<ResourcePool<T>> currentThrdPool;
    protected final Deque<ResourcePool<T>> pools;
    protected volatile boolean closed;

    public ThreadLocalPool(Resource<T> resource, int poolSize) {
        pools = new ArrayDeque<>();
        this.currentThrdPool = ThreadLocal.withInitial(() -> {
            synchronized (this) {
                var pool = new SimpleResourcePool<>(resource, poolSize);
                pools.push(pool);
                return pool;
            }
        });
    }

    @Override
    public Future<Recyclable<T>> acquireResource() {
        if (closed) throw new IllegalStateException("The pool is already closed");
        var future = currentThrdPool.get().acquireResource();
        if (DETECT_RECYCLABLE_THREAD_LEAK) return future.map(PinnedRecyclable::new);
        return future;
    }

    @Override
    public synchronized void close() throws Exception {
        if (closed) throw new IllegalStateException("The pool is already closed");
        closed = true;
        for (ResourcePool<T> pool : pools) {
            pool.close();
        }
    }

    @RequiredArgsConstructor
    @Log4j2
    static class PinnedRecyclable<T> implements Recyclable<T> {
        protected final Recyclable<T> recyclable;
        protected final Thread creationThread = Thread.currentThread();

        @Override
        public T unwrap() {
            mustSameThread();
            return recyclable.unwrap();
        }

        @Override
        public void recycle() {
            mustSameThread();
            recyclable.recycle();
        }

        void mustSameThread() {
            if (creationThread != Thread.currentThread()) {
                throw new IllegalStateException("A thread-local resource has been leaked to another thread!" +
                        " From: " + creationThread + ", leaked to " + Thread.currentThread());
            }
        }
    }
}

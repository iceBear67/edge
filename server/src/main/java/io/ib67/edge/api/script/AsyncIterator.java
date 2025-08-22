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

package io.ib67.edge.api.script;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;

import java.util.Iterator;

@ExportToScript
public class AsyncIterator<T> implements Iterator<Future<T>> {
    protected final Vertx vertx;
    protected final Iterator<T> iterator;
    protected final int stepsPerLoop;
    protected int steps;

    public AsyncIterator(Vertx vertx, Iterator<T> iterator, int steps) {
        this.vertx = vertx;
        this.iterator = iterator;
        this.stepsPerLoop = steps;
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public Future<T> next() {
        if(++steps % stepsPerLoop == 0){
            Promise<T> promise = Promise.promise();
            var next = iterator.next();
            vertx.runOnContext(h -> promise.succeed(next));
            return promise.future();
        }
        return Future.succeededFuture(iterator.next());
    }
}

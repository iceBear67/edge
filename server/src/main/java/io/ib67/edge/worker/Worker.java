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

package io.ib67.edge.worker;

import io.ib67.edge.serializer.HttpRequestBox;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.HttpServerRequest;
import lombok.extern.log4j.Log4j2;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Worker 负责处理 eventbus 上订阅的信息以保持总是在同一个线程上。
 */
@Log4j2
public class Worker extends AbstractVerticle {
    protected static final AtomicInteger COUNTER = new AtomicInteger();
    protected final String consumerId;
    protected MessageConsumer<HttpRequestBox> busHandle;
    protected final Runnable onClose;

    public Worker(Runnable onClose) {
        this.consumerId = "worker" + "-" + COUNTER.incrementAndGet();
        this.onClose = onClose;
    }

    @Override
    public void start() {
        busHandle = vertx.eventBus().consumer(consumerId, this::handleRequest0);
    }

    @Override
    public void stop(){
        try {
            onClose.run();
        } catch (Throwable e) {
            log.error("Error occurred when closing resources for a worker", e);
        }
        busHandle.unregister();
    }

    protected void handleRequest0(Message<HttpRequestBox> objectMessage) {
    }

    public void handleRequest(Vertx vertx, HttpServerRequest objectMessage) {
        vertx.eventBus().publish(consumerId, new HttpRequestBox(objectMessage));
    }
}

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

import io.ib67.edge.api.RequestHandler;
import io.ib67.edge.api.http.EdgeRequest;
import io.ib67.edge.script.context.ScriptContext;
import io.ib67.edge.serializer.HttpRequestBox;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

/**
 * ScriptWorker 将请求转发给关联起来的脚本。脚本需要符合 esm 规范返回 export {}
 */
@Log4j2
public class ScriptWorker extends Worker {
    protected final ScriptContext context;
    protected RequestHandler handler;

    public ScriptWorker(ScriptContext context, Runnable onClose) {
        super(onClose);
        this.context = context;
    }

    @SneakyThrows
    @Override
    public void start() {
        super.start();
        if(!context.isInitialized()) context.init();
        var binding = context.getScriptContext().getBindings("js");
        binding.putMember("log", log);
        handler = context.getExportedMembers().get("handleRequest").as(RequestHandler.class);
        context.onLifecycleEvent("start");
    }

    @Override
    public void stop(Promise<Void> stopPromise) throws Exception {
        super.stop(stopPromise);
        context.onLifecycleEvent("stop");
    }

    @Override
    protected void handleRequest0(Message<HttpRequestBox> objectMessage) {
        var req = objectMessage.body().request();
        try {
            handler.handleRequest((EdgeRequest) req);
        } catch (Exception throwable) {
            req.response().setStatusCode(500).end("Script Server Error");
            log.error("Error handling request {}/{}", req.getHeader("Host"), req.path(), throwable);
        }
    }
}

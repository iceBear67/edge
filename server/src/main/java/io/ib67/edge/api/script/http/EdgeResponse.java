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

package io.ib67.edge.api.script.http;

import io.ib67.edge.api.script.MixinHelper;
import io.ib67.edge.mixin.Mixin;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;

@Mixin(HttpServerResponse.class)
public interface EdgeResponse extends MixinHelper<HttpServerResponse> {
    default EdgeResponse setStatusCode(int statusCode) {
        $().setStatusCode(statusCode);
        return this;
    }

    default Future<Void> write(String chunk) {
        return $().write(chunk);
    }

    default Future<Void> write(Buffer buffer) {
        return $().write(buffer);
    }

    default Future<Void> send(Buffer buffer) {
        return $().send(buffer);
    }

    default Future<Void> send(String chunk) {
        return $().send(chunk);
    }

    default EdgeResponse end(String chunk) {
        $().end(chunk);
        return this;
    }

    default EdgeResponse end() {
        $().end();
        return this;
    }

    default Future<Void> end(Buffer buffer) {
        return $().end(buffer);
    }

    default EdgeResponse putHeader(String name, String value) {
        $().putHeader(name, value);
        return this;
    }

    default MultiMap headers() {
        return $().headers();
    }
}

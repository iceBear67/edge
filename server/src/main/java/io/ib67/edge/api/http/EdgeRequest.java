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

package io.ib67.edge.api.http;

import io.ib67.edge.api.MixinHelper;
import io.ib67.edge.mixin.Mixin;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;

@Mixin(HttpServerRequest.class)
public interface EdgeRequest extends MixinHelper<HttpServerRequest> {

    default Future<Buffer> body(){
        return $().body();
    }

    default String absoluteURI(){
        return $().absoluteURI();
    }

    default HttpMethod method() {
        return $().method();
    }

    default MultiMap params(){
        return $().params();
    }

    default MultiMap headers() {
        return $().headers();
    }

    default EdgeResponse response() {
        return (EdgeResponse) $().response();
    }
}

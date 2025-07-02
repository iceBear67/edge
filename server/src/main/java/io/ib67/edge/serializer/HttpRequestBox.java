package io.ib67.edge.serializer;

import io.vertx.core.http.HttpServerRequest;

/**
 * Tricking the eventbus.
 * @param request
 */
public record HttpRequestBox(HttpServerRequest request) {
}

package io.ib67.edge.api;

import io.vertx.core.http.HttpServerRequest;

@FunctionalInterface
public interface RequestHandler {
    void handleRequest(HttpServerRequest objectMessage);
}

package io.ib67.edge;

import io.vertx.core.Vertx;
import org.graalvm.polyglot.Engine;

public class Main {
    public static void main(String[] args) {
        var vertx = Vertx.vertx();
        vertx.deployVerticle(new ServerVerticle(Engine.create()));
    }
}

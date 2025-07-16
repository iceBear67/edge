module edge.server {
    requires com.google.common.jimfs;
    requires io.vertx.web;
    requires io.vertx.core;
    requires org.apache.logging.log4j;
    requires edge.codegen;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.core;
    requires static lombok;
    requires org.graalvm.polyglot;
    exports io.ib67.edge.api;
    exports io.ib67.edge.api.future;
    exports io.ib67.edge.api.http;
    exports io.ib67.edge;
    exports io.ib67.edge.config;
    exports io.ib67.edge.worker;

    // script runtimes
    exports io.ib67.edge.script;
    exports io.ib67.edge.script.context;
    exports io.ib67.edge.script.exception;
    exports io.ib67.edge.script.io;
    exports io.ib67.edge.script.locator;
}
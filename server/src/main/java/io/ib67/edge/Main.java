package io.ib67.edge;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.ib67.edge.config.ServerConfig;
import io.ib67.edge.script.ModuleRuntime;
import io.ib67.edge.script.locator.DirectoryModuleLocator;
import io.vertx.core.Vertx;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
public class Main {
    private static final String CONFIG_PATH = System.getProperty("EDGE_CONFIG_PATH", "config.json");

    @SneakyThrows
    public static void main(String[] args) {
        log.info("Initializing edge server...");
        var om = new ObjectMapper()
                .configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true)
                .configure(JsonParser.Feature.ALLOW_COMMENTS, true)
                .configure(JsonParser.Feature.IGNORE_UNDEFINED, true)
                .configure(SerializationFeature.INDENT_OUTPUT, true);
        var serverConfig = om.readValue(new File(CONFIG_PATH), ServerConfig.class);
        var vertx = Vertx.vertx();
        var engine = Engine.newBuilder()
                .in(InputStream.nullInputStream())
                .out(OutputStream.nullOutputStream()) // todo logging management
                .options(serverConfig.engineOptions())
                .build();
        Files.createDirectories(Path.of(serverConfig.runtime().pathLibraries()));
        Files.createDirectories(Path.of(serverConfig.runtime().pathLibraryCache()));
        System.setProperty("edge.isolatedruntime.stub.cachedir", serverConfig.runtime().pathLibraryCache());
        var runtime = new ModuleRuntime(
                engine,
                FileSystems.getDefault(),
                new DirectoryModuleLocator(Path.of(serverConfig.runtime().pathLibraries())),
                HostAccess.ALL
        );
        var serverVerticle = new ServerVerticle(runtime);
        vertx.deployVerticle(serverVerticle);
    }
}

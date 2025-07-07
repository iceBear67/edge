package io.ib67.edge;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.ib67.edge.config.ServerConfig;
import io.ib67.edge.script.IsolatedRuntime;
import io.ib67.edge.script.locator.DirectoryModuleLocator;
import io.vertx.core.Vertx;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.graalvm.polyglot.Engine;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Log4j2
public class Main {
    private static final String CONFIG_PATH = System.getProperty("edge.config", "config.json");

    @SneakyThrows
    public static void main(String[] args) {
        log.info("Initializing edge server...");
        var om = new ObjectMapper()
                .configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true)
                .configure(JsonParser.Feature.ALLOW_COMMENTS, true)
                .configure(JsonParser.Feature.IGNORE_UNDEFINED, true)
                .configure(SerializationFeature.INDENT_OUTPUT, true);
        var configFile = new File(CONFIG_PATH);
        if (!configFile.exists()) {
            om.writeValue(configFile, ServerConfig.defaultConfig());
        }
        var serverConfig = om.readValue(configFile, ServerConfig.class);
        var vertx = Vertx.vertx();
        var engine = Engine.newBuilder()
                .in(InputStream.nullInputStream())
                .out(OutputStream.nullOutputStream()) // todo logging management
                .options(serverConfig.engineOptions())
                .build();
        Files.createDirectories(Path.of(serverConfig.runtime().pathLibraries()));
        log.info("Initializing runtime...");
        var runtime = new IsolatedRuntime(
                engine,
                new DirectoryModuleLocator(Path.of(serverConfig.runtime().pathLibraries())),
                IsolatedRuntime.hostContainerAccess().build()
        );
        runtime.setHostContextOptions(serverConfig.runtime().hostContextOptions());
        runtime.setGuestContextOptions(serverConfig.runtime().guestContextOptions());
        log.info("Deploying server verticle...");
        var serverVerticle = new ServerVerticle(serverConfig.listenHost(), serverConfig.listenPort(), runtime);
        vertx.deployVerticle(serverVerticle);
    }
}

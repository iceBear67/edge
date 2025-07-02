package io.ib67.edge;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.ib67.edge.serializer.SourceDeserializer;
import io.ib67.edge.serializer.SourceSerializer;
import lombok.SneakyThrows;
import org.graalvm.polyglot.Source;
import org.junit.jupiter.api.Test;

public class TestSourceSerializer {
    @SneakyThrows
    @Test
    public void test() {
        var source = Source.newBuilder("js", """
                function handleRequest(request) {
                    request.response().end("hello");
                }
                export {
                  handleRequest
                };
                """, "test.mjs").build();
        var module = new SimpleModule();

        module.addSerializer(new SourceSerializer());
        module.addDeserializer(Source.class, new SourceDeserializer());
        var om = new ObjectMapper().registerModules(module).writerWithDefaultPrettyPrinter();
        System.out.println(om.writeValueAsString(source));
    }
}

package io.ib67.edge;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.ib67.edge.script.ScriptOption;
import org.graalvm.polyglot.Source;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class DeployTest {
    @Test
    public void test() throws IOException {
        var source = Source.newBuilder("js", """
                function handleRequest(request) {
                    request.response().end("hello");
                }
                export {
                  handleRequest
                };
                """, "test.mjs").build();
        var deployment = new Deployment(
                "test",
                "0.0.1",
                source,
                List.of(
                        new ScriptOption.ContextOption("js.esm-eval-returns-exports", "true"),
                        new ScriptOption.PathAccess(Path.of(""), true)
                )
        );
        var om = new ObjectMapper().writerWithDefaultPrettyPrinter();
        System.out.println(om.writeValueAsString(deployment));
    }
}

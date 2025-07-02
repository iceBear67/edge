package io.ib67.edge;

import lombok.SneakyThrows;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.junit.jupiter.api.Test;

public class TestGraalJS {
    @SneakyThrows
    @Test
    public void test() {
        var ctx = Context.newBuilder("js")
                .option("js.esm-eval-returns-exports", "true")
                .allowAllAccess(true).build();
        var exports = ctx.eval(Source.newBuilder("js", """
                function handleRequest(request) {
                    request.response().end("hello");
                }
                export {
                  handleRequest
                };
                ""","test.mjs").build());
        System.out.println(1);
    }
}

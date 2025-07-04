package io.ib67.edge.script.context;

import lombok.SneakyThrows;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Source;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class IncrementalModuleContextTest {
    Context context;
    static Engine engine;

    @BeforeAll
    static void setupEngine() {
        engine = Engine.create();
    }

    @AfterAll
    static void teardownEngine() {
        engine.close();
    }

    @BeforeEach
    void setup() {
        context = Context.newBuilder()
                .option("js.esm-eval-returns-exports", "true")
                .allowHostAccess(HostAccess.ALL)
                .allowHostClassLookup(it -> true)
                .engine(engine).build();
    }

    @AfterEach
    void teardownContext() {
        context.close(true);
    }

    @SneakyThrows
    @Test
    void loadModules() {
        var moduleSource = Source.newBuilder("js", """
                let a = 1;
                let b = 2;
                export {a,b}
                """, "test.mjs").build();
        var scriptContext = new IncrementalModuleContext(context);
        scriptContext.evalModule("a", moduleSource);
        scriptContext.evalModule("b", moduleSource);
        assertEquals(2, scriptContext.getExportedMembers().size());

        assertEquals(1, scriptContext.getModuleExports().get("a").getMember("a").asInt());
        assertEquals(2, scriptContext.getModuleExports().get("a").getMember("b").asInt());
        assertEquals(1, scriptContext.getModuleExports().get("b").getMember("a").asInt());
        assertEquals(2, scriptContext.getModuleExports().get("b").getMember("b").asInt());
    }

}
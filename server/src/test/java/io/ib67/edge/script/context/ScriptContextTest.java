package io.ib67.edge.script.context;

import lombok.SneakyThrows;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Source;
import org.junit.jupiter.api.*;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScriptContextTest {
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
                .allowHostClassLookup(it->true)
                .engine(engine).build();
    }

    @AfterEach
    void teardown(){
        context.close(true);
    }

    @SneakyThrows
    @Test
    void testCreation() {
        var source = Source.newBuilder("js", """
                let a = 1;
                let b = 2;
                export {a,b};
                """, "test.mjs").build();
        var scriptContext = new ScriptContext(context, source);
        assertEquals(2, scriptContext.getExportedMembers().size());
        assertEquals(1, scriptContext.getExportedMembers().get("a").asInt());
        assertEquals(2, scriptContext.getExportedMembers().get("b").asInt());
    }

    @SneakyThrows
    @Test
    void testLifecycle() {
        var onCleanRun = new AtomicBoolean(false);
        var testHandler = (Runnable) () -> onCleanRun.getAndSet(true);
        var source = Source.create("js", """
                context.on('customEvent', () => testHandler.run())
                context.on('clean', () => testHandler.run())
                """);
        context.getBindings("js").putMember("testHandler", testHandler);
        var sc = new ScriptContext(context, source);
        sc.onLifecycleEvent("customEvent");
        assertTrue(onCleanRun.get());
        onCleanRun.set(false);
        sc.close();
        assertTrue(onCleanRun.get());
    }

}
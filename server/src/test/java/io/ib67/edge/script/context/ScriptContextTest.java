/*
 *    Copyright 2025 iceBear67 and Contributors
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

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
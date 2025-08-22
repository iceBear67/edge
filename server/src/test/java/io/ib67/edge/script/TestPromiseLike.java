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

package io.ib67.edge.script;

import io.ib67.edge.api.script.AsyncIterator;
import io.ib67.edge.api.script.ExportToScript;
import io.ib67.edge.api.script.future.Thenable;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.proxy.ProxyIterable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/**
 * This test was moved to launcher for
 */
public class TestPromiseLike {
    @BeforeAll
    public static void assertThenableWorking() {
        Assertions.assertInstanceOf(Thenable.class, Future.succeededFuture());
    }

    @Test
    public void testAsyncIterator() {
        var vertx = mock(Vertx.class);
        AtomicInteger c = new AtomicInteger();
        doAnswer(invo -> {
            ((Handler<Void>) invo.getArguments()[0]).handle(null);
            c.incrementAndGet();
            return null;
        }).when(vertx).runOnContext(any(Handler.class));

        var runtime = new ScriptRuntime(Engine.newBuilder().err(System.err).build()) {
            @Override
            protected HostAccess getHostAccess() {
                return HostAccess.newBuilder()
                        .allowAccessAnnotatedBy(ExportToScript.class)
                        .build();
            }
        };
        var result = new ArrayList<String>();
        var elements = List.of("a", "b", "c");
        var asyncIterator = ProxyIterable.from(() -> (Iterator<Object>)(Object) new AsyncIterator<>(vertx, elements.iterator(), 1));
        var source = Source.create("js", """
                (async function(){
                try{
                    for await (const element of iterator){
                        result.collect(element);
                        console.log(element);
                    }
                }catch(error){
                    console.error(error)
                }
                })();
                """);
        var context = Assertions.assertDoesNotThrow(() -> runtime.create(Source.create("js", ""), binding -> {
            binding.putMember("result", new TestResultCollector(result::add));
            binding.putMember("iterator", asyncIterator);
        }));
        Assertions.assertDoesNotThrow(context::init);
        context.eval(source);

        assertEquals(c.get(), result.size());
        assertEquals(elements, result);
    }

    public record TestResultCollector(Consumer<String> collector){
        @ExportToScript
        public void collect(String result){
            collector.accept(result);
        }
    }
}

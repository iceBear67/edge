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

package io.ib67.edge;

import io.ib67.edge.api.future.Thenable;
import io.ib67.edge.enhance.AnnotationEnhancer;
import io.ib67.edge.enhance.EdgeClassEnhancer;
import io.vertx.core.Future;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.nio.file.Files;
import java.nio.file.Path;

import static java.lang.invoke.MethodHandles.lookup;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestEnhancer implements Opcodes {
    @SneakyThrows
    @Test
    public void testAnnotation() {
        var rule = """
                @java/lang/Deprecated
                TYPE io/ib67/edge/EnhancerTest_$$$
                FIELD Lio/ib67/edge/EnhancerTest_$$$;test
                METHOD Lio/ib67/edge/EnhancerTest_$$$;testM()I
                """;
        var enhancer = new EdgeClassEnhancer()
                .addTransformer(
                        parent -> new AnnotationEnhancer(ASM9, parent, new AnnotationRuleParser().parse(rule)).setVerbose(true)
                );
        var cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(
                V21,
                ACC_PUBLIC,
                "io/ib67/edge/EnhancerTest_$$$",
                null, "java/lang/Object", null
        );
        var method = cw.visitMethod(ACC_PUBLIC, "testM", "()I", null, null);
        method.visitInsn(ICONST_0);
        method.visitInsn(IRETURN);
        method.visitMaxs(1,1);
        method.visitEnd();
        cw.visitField(ACC_PUBLIC + ACC_STATIC, "test", "Ljava/lang/String;", null, null);
        var clazzByte = enhancer.enhance(null, cw.toByteArray());
        Files.write(Path.of("test.class"), clazzByte);
        var enhanced = lookup().defineClass(clazzByte);
        assertTrue(enhanced.isAnnotationPresent(Deprecated.class));
        assertTrue(enhanced.getMethod("testM").isAnnotationPresent(Deprecated.class));
        assertTrue(enhanced.getField("test").isAnnotationPresent(Deprecated.class));
    }

    @SneakyThrows
    @Test
    public void testThenableAttach() {
        assertTrue(Thenable.class.isAssignableFrom(Future.class));
    }
}

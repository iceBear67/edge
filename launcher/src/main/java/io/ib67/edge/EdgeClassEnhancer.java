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

import lombok.SneakyThrows;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static io.ib67.edge.EdgeTransformerAgent.DUMP;

public class EdgeClassEnhancer implements Opcodes {
    protected final List<Function<ClassVisitor, ClassVisitor>> transformers;

    public EdgeClassEnhancer() {
        this.transformers = new ArrayList<>();
    }

    public EdgeClassEnhancer addTransformer(Function<ClassVisitor, ClassVisitor> supplier) {
        this.transformers.add(supplier);
        return this;
    }

    @SneakyThrows
    public byte[] enhance(String className, byte[] bytes) {
        var cv = new ClassReader(bytes);
        var cw = new ClassWriter(cv, 0);
        ClassVisitor visitor = cw;
        for (Function<ClassVisitor, ClassVisitor> transformer : transformers) {
            visitor = transformer.apply(visitor);
        }
        cv.accept(visitor, 0);
        visitor.visitEnd();
        var enhanced = cw.toByteArray();
        if (DUMP != null) {
            var path = Paths.get(DUMP).resolve(className + ".class");
            Files.createDirectories(path.getParent());
            Files.write(path, enhanced);
        }
        return enhanced;
    }
}

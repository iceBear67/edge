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

package io.ib67.edge.enhance;

import io.ib67.edge.AnnotationRuleParser;
import lombok.SneakyThrows;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.ToIntFunction;

import static org.objectweb.asm.ClassReader.SKIP_CODE;

public class EdgeClassEnhancer implements Opcodes {
    protected static final String ANNOTATION_RULE_PATH = System.getenv("EDGE_ANNOTATION_RULES");
    protected static final boolean VERBOSE = System.getenv("EDGE_PRINT_ANNOTATED") != null;
    protected static final String DUMP = System.getenv("EDGE_LAUNCHER_DUMP");
    protected final Map<ToIntFunction<String>, Function<ClassVisitor, ClassVisitor>> transformers;

    public static EdgeClassEnhancer create() {
        var classLoader = EdgeClassEnhancer.class.getClassLoader();
        var enhancer = new EdgeClassEnhancer();
        try (var in = ANNOTATION_RULE_PATH == null ?
                classLoader.getResourceAsStream("edge_annotation_rules.txt")
                : new FileInputStream(ANNOTATION_RULE_PATH)) {
            if (in != null) {
                var ruleData = in.readAllBytes();
                var rule = new AnnotationRuleParser().parse(new String(ruleData));
                if (VERBOSE) System.out.println("Loaded annotation rule: " + rule);
                enhancer.addTransformer(it -> SKIP_CODE, cw ->
                        new AnnotationEnhancer(ASM9, cw, rule).setVerbose(VERBOSE));
            } else {
                System.err.println("Failed to load annotation rules");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        enhancer.addTransformer(name -> {
            if ("io/vertx/core/Future".equals(name)) {
                return SKIP_CODE;
            }
            return -1;
        }, it -> new VertxEnhancer(ASM9, it));
        return enhancer;
    }

    public EdgeClassEnhancer() {
        this.transformers = new HashMap<>();
    }

    public EdgeClassEnhancer addTransformer(ToIntFunction<String> predicate, Function<ClassVisitor, ClassVisitor> supplier) {
        this.transformers.put(predicate, supplier);
        return this;
    }

    @SneakyThrows
    public byte[] enhance(String className, byte[] bytes) {
        ClassReader cv = null;
        if (className == null) {
            cv = new ClassReader(bytes);
            className = cv.getClassName();
        } else {
            className = className.replace('.', '/');
        }
        ClassVisitor last = null;
        ClassWriter cw = null;
        for (var entry : transformers.entrySet()) {
            var option = entry.getKey().applyAsInt(className);
            if (option != -1) {
                if (cv == null) cv = new ClassReader(bytes);
                if (cw == null) {
                    cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                    last = cw;
                }
                last = entry.getValue().apply(last);
            }
        }
        if (cv == null || cw == null) return bytes;
        cv.accept(last, 0);
        if (DUMP != null) {
            var path = Paths.get(DUMP).resolve(className+".class");
            Files.createDirectories(path.getParent());
            Files.write(path, cw.toByteArray());
        }
        return cw.toByteArray();
    }
}

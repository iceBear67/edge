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

import io.ib67.edge.enhance.AnnotationEnhancer;
import io.ib67.edge.enhance.EdgeClassEnhancer;
import io.ib67.edge.enhance.MixinEnhancer;
import lombok.SneakyThrows;
import org.objectweb.asm.Opcodes;

import java.io.FileInputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.nio.file.Path;
import java.security.ProtectionDomain;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EdgeTransformerAgent implements ClassFileTransformer, Opcodes {
    public static final String ANNOTATION_RULE_PATH = System.getenv("EDGE_LAUNCHER_ANNOTATION_RULES");
    public static final String MIXIN_PATH = System.getenv("EDGE_LAUNCHER_MIXIN_PATH");
    public static final Set<String> VERBOSE = Optional.ofNullable(System.getenv("EDGE_LAUNCHER_VERBOSE"))
            .stream().flatMap(it -> Stream.of(it.split(",")))
            .collect(Collectors.toSet());
    public static final String VERBOSE_TOPIC_LOAD = "load";
    public static final String VERBOSE_TOPIC_ANNOTATE = "annotate";
    public static final String VERBOSE_TOPIC_MIXIN = "mixin";
    public static final String DUMP = System.getenv("EDGE_LAUNCHER_DUMP");
    protected final EdgeClassEnhancer enhancer;

    public EdgeTransformerAgent(EdgeClassEnhancer enhancer) {
        this.enhancer = enhancer;
    }

    public static void premain(String agentArgs, Instrumentation inst) throws ClassNotFoundException {
        System.out.println("EdgeTransformerAgent is loaded. CWD: " + Path.of(".").toAbsolutePath().normalize());
        Class.forName("org.objectweb.asm.ClassReader"); // resolves classloading deadlock
        Class.forName("org.objectweb.asm.ClassWriter");
        inst.addTransformer(new EdgeTransformerAgent(createEnhancer()));
    }

    @SneakyThrows
    private static EdgeClassEnhancer createEnhancer() {
        var classLoader = EdgeClassEnhancer.class.getClassLoader();
        var enhancer = new EdgeClassEnhancer();
        try (var in = ANNOTATION_RULE_PATH == null ?
                classLoader.getResourceAsStream("edge_annotation_rules.txt")
                : new FileInputStream(ANNOTATION_RULE_PATH)) {
            if (in == null) {
                System.err.println("Failed to load annotation rules");
            } else {
                var ruleData = in.readAllBytes();
                var rule = new AnnotationRuleParser().parse(new String(ruleData));
                if (VERBOSE.contains(VERBOSE_TOPIC_LOAD)) System.out.println("Loaded annotation " + rule.size() + " rules");
                enhancer.addTransformer(cw -> new AnnotationEnhancer(ASM9, cw, rule).setVerbose(VERBOSE.contains(VERBOSE_TOPIC_ANNOTATE)));
            }
        }
        try (var in = MIXIN_PATH == null
                ? classLoader.getResourceAsStream("META-INF/mixins.txt")
                : new FileInputStream(MIXIN_PATH)) {
            if (in == null) {
                System.err.println("Failed to load mixins");
            } else {
                var mixinData = MixinParser.parse(new String(in.readAllBytes()));
                enhancer.addTransformer(cw -> new MixinEnhancer(mixinData, ASM9, cw));
            }
        }
        return enhancer;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        return enhancer.enhance(className, classfileBuffer);
    }
}

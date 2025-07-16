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

import io.ib67.edge.enhancer.AnnotationEnhancer;
import io.ib67.edge.enhancer.MixinEnhancer;
import io.ib67.edge.parser.AnnotationRuleParser;
import io.ib67.edge.parser.MixinParser;
import lombok.SneakyThrows;
import org.objectweb.asm.Opcodes;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.nio.file.Path;
import java.security.ProtectionDomain;
import java.util.Objects;
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
        if (VERBOSE.contains(VERBOSE_TOPIC_LOAD))
            System.out.println("EdgeTransformerAgent is loaded. CWD: " + Path.of(".").toAbsolutePath().normalize());
        Class.forName("org.objectweb.asm.ClassReader"); // resolves classloading deadlock
        Class.forName("org.objectweb.asm.ClassWriter");
        inst.addTransformer(new EdgeTransformerAgent(createEnhancer()));
    }

    @SneakyThrows
    private static EdgeClassEnhancer createEnhancer() {
        var enhancer = new EdgeClassEnhancer();
        try (var in = readFileOrResource(ANNOTATION_RULE_PATH, "edge_annotation_rules.txt")) {
            var ruleData = in.readAllBytes();
            var rule = AnnotationRuleParser.parse(new String(ruleData));
            if (VERBOSE.contains(VERBOSE_TOPIC_LOAD))
                System.out.println("Loaded " + rule.size() + " annotation rules");
            enhancer.addTransformer(cw -> new AnnotationEnhancer(ASM9, cw, rule).setVerbose(VERBOSE.contains(VERBOSE_TOPIC_ANNOTATE)));
        }
        try (var in = readFileOrResource(MIXIN_PATH, "META-INF/mixins.txt")) {
            var mixinData = MixinParser.parse(new String(in.readAllBytes()));
            enhancer.addTransformer(cw -> new MixinEnhancer(mixinData, ASM9, cw).setVerbose(VERBOSE.contains(VERBOSE_TOPIC_MIXIN)));
        }
        return enhancer;
    }

    private static InputStream readFileOrResource(String path, String resource) throws FileNotFoundException {
        return Objects.requireNonNull(path == null ? EdgeClassEnhancer.class.getClassLoader().getResourceAsStream(resource)
                : new FileInputStream(path), "Failed to load " + resource + " or " + path);
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        return enhancer.enhance(className, classfileBuffer);
    }
}

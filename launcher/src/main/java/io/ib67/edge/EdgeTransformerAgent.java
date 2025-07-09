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

import io.ib67.edge.enhance.EdgeClassEnhancer;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.nio.file.Path;
import java.security.ProtectionDomain;

public class EdgeTransformerAgent implements ClassFileTransformer {
    protected final EdgeClassEnhancer enhancer;

    public EdgeTransformerAgent(EdgeClassEnhancer enhancer) {
        this.enhancer = enhancer;
    }

    public static void premain(String agentArgs, Instrumentation inst) throws ClassNotFoundException {
        System.out.println("EdgeTransformerAgent is loaded. CWD: "+ Path.of(".").toAbsolutePath().normalize());
        Class.forName("org.objectweb.asm.ClassReader"); // resolves classloading deadlock
        Class.forName("org.objectweb.asm.ClassWriter");
        inst.addTransformer(new EdgeTransformerAgent(EdgeClassEnhancer.create()));
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        return enhancer.enhance(className, classfileBuffer);
    }
}

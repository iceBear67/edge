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

    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("EdgeTransformerAgent is loaded. CWD: "+ Path.of(".").toAbsolutePath().normalize());
        inst.addTransformer(new EdgeTransformerAgent(EdgeClassEnhancer.create()));
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        return enhancer.enhance(className, classfileBuffer);
    }
}

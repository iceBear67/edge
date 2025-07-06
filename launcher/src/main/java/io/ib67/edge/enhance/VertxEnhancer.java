package io.ib67.edge.enhance;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;

public class VertxEnhancer extends ClassVisitor implements Opcodes {
    protected VertxEnhancer(int api, ClassVisitor classVisitor) {
        super(api, classVisitor);
    }

    protected boolean shouldEnhance(String className) {
        return "io/vertx/core/Future".equals(className);
    }

    // void then(Value onResolve, Value onReject);
    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] _interfaces) {
        if (!shouldEnhance(name)) {
            super.visit(version, access, name, signature, superName, _interfaces);
        } else {
            _interfaces = _interfaces == null ? new String[0] : _interfaces;
            var interfaces = Arrays.copyOf(_interfaces, _interfaces.length + 1);
            interfaces[interfaces.length - 1] = "io/ib67/edge/mixin/Thenable";
            super.visit(version, access, name, signature, superName, interfaces);
        }
    }
}

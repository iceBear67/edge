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
            interfaces[interfaces.length - 1] = "io/ib67/edge/api/Thenable";
            super.visit(version, access, name, signature, superName, interfaces);
        }
    }
}

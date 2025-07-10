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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static io.ib67.edge.EdgeTransformerAgent.VERBOSE;
import static io.ib67.edge.EdgeTransformerAgent.VERBOSE_TOPIC_MIXIN;

public class MixinEnhancer extends ClassVisitor implements Opcodes {
    protected final Map<String, Set<String>> interfaces;

    public MixinEnhancer(Map<String, Set<String>> mixins, int api, ClassVisitor classVisitor) {
        super(api, classVisitor);
        this.interfaces = mixins;
    }

    public MixinEnhancer addInterfaceMixin(String interfaceName, String target) {
        interfaces.computeIfAbsent(interfaceName, k -> new HashSet<>()).add(target);
        return this;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] _interfaces) {
        _interfaces = _interfaces == null ? new String[0] : _interfaces;
        for (var entry : interfaces.entrySet()) {
            if (entry.getValue().contains(name)) {
                _interfaces = Arrays.copyOf(_interfaces, _interfaces.length + 1);
                _interfaces[_interfaces.length - 1] = entry.getKey();
                if(VERBOSE.contains(VERBOSE_TOPIC_MIXIN)){
                    System.out.println("Mixin "+entry.getKey()+" applied to "+entry.getValue());
                }
            }
        }
        super.visit(version, access, name, signature, superName, _interfaces);
    }
}

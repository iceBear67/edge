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

import lombok.Getter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

import java.util.List;

public class AnnotationEnhancer extends ClassVisitor {
    public enum EnhanceType {
        CLASS_ALL, CLASS, METHOD, FIELD
    }

    public interface EnhanceRule {
        String descriptor();

        boolean shouldEnhance(EnhanceType type, String descriptor);
    }

    protected final List<EnhanceRule> rules;
    @Getter
    private String currentVisitingClass;
    @Getter
    protected boolean verbose;
    protected boolean allowAll;

    public AnnotationEnhancer(int api, ClassVisitor parent, List<EnhanceRule> rules) {
        super(api, parent);
        this.rules = rules;
    }

    public AnnotationEnhancer setVerbose(boolean verbose) {
        this.verbose = verbose;
        return this;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        currentVisitingClass = name;
        allowAll = false;
        for (EnhanceRule rule : rules) {
            if(rule.shouldEnhance(EnhanceType.CLASS_ALL, name)){
                allowAll = true;
            }
            if (allowAll || rule.shouldEnhance(EnhanceType.CLASS, name)) {
                visitAnnotation(rule.descriptor(), true);
                if (verbose) {
                    System.out.println("TYPE "+name);
                }
                break;
            }
        }
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public void visitEnd() {
        allowAll = false;
        super.visitEnd();
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        var method = super.visitMethod(access, name, descriptor, signature, exceptions);
        var desc = currentVisitingClass + "." + name + descriptor;
        for (EnhanceRule rule : rules) {
            if (allowAll ||rule.shouldEnhance(EnhanceType.METHOD, desc)) {
                method.visitAnnotation(rule.descriptor(), true);
                if (verbose) {
                    System.out.println("METHOD "+desc);
                }
                break;
            }
        }
        return method;
    }


    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        var field = super.visitField(access, name, descriptor, signature, value);
        var desc = currentVisitingClass + "." + name;
        for (EnhanceRule rule : rules) {
            if (allowAll || rule.shouldEnhance(EnhanceType.FIELD, desc)) {
                field.visitAnnotation(rule.descriptor(), true);
                if (verbose) {
                    System.out.println("FIELD "+desc);
                }
                break;
            }
        }
        return field;
    }
}

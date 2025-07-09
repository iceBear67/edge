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

package io.ib67.edge.ap;

import javax.annotation.processing.Messager;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.Set;

public class MixinAnnotationVisitor implements AnnotationValueVisitor<Void, Set<TypeElement>> {
    protected final Messager messager;

    public MixinAnnotationVisitor(Messager messager) {
        this.messager = messager;
    }

    @Override
    public Void visitType(TypeMirror t, Set<TypeElement> typeElements) {
        if (t instanceof DeclaredType dt && dt.asElement() instanceof TypeElement typeElement) {
            typeElements.add(typeElement);
        } else {
            messager.printWarning("TypeMirror which is not a DeclaredType is found at " + t);
        }
        return null;
    }

    //@formatter:off
    @Override public Void visit(AnnotationValue av, Set<TypeElement> typeElements) {return null;}
    @Override public Void visitBoolean(boolean b, Set<TypeElement> typeElements) {return null;}
    @Override public Void visitByte(byte b, Set<TypeElement> typeElements) {return null;}
    @Override public Void visitChar(char c, Set<TypeElement> typeElements) {return null;}
    @Override public Void visitDouble(double d, Set<TypeElement> typeElements) {return null;}
    @Override public Void visitFloat(float f, Set<TypeElement> typeElements) {return null;}
    @Override public Void visitInt(int i, Set<TypeElement> typeElements) {return null;}
    @Override public Void visitLong(long i, Set<TypeElement> typeElements) {return null;}
    @Override public Void visitShort(short s, Set<TypeElement> typeElements) {return null;}
    @Override public Void visitString(String s, Set<TypeElement> typeElements) {return null;}
    @Override public Void visitEnumConstant(VariableElement c, Set<TypeElement> typeElements) {return null;}
    @Override public Void visitAnnotation(AnnotationMirror a, Set<TypeElement> typeElements) {return null;}
    @Override public Void visitArray(List<? extends AnnotationValue> vals, Set<TypeElement> typeElements) {return null;}
    @Override public Void visitUnknown(AnnotationValue av, Set<TypeElement> typeElements) {return null;}
    //@formatter:on
}

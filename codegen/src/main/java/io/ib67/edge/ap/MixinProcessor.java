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

import lombok.SneakyThrows;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static javax.tools.StandardLocation.CLASS_OUTPUT;

@SupportedAnnotationTypes("io.ib67.edge.mixin.Mixin")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class MixinProcessor extends AbstractProcessor {
    protected MixinAnnotationVisitor annotationVisitor;

    @SneakyThrows
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        var messager = processingEnv.getMessager();
        var names = new HashMap<String, Set<TypeElement>>();
        if (annotations.isEmpty()) {
            return false;
        }
        for (TypeElement annotation : annotations) {
            var allAnnotated = roundEnv.getElementsAnnotatedWith(annotation);
            for (Element element : allAnnotated) {
                if (element.getKind() != ElementKind.INTERFACE) {
                    messager.printError("Only interfaces can be annotated with @Mixin", element);
                    return false;
                }
                TypeElement type = (TypeElement) element;
                for (AnnotationMirror annotationMirror : type.getAnnotationMirrors()) {
                    if (annotationMirror.getAnnotationType().asElement().equals(annotation)) { // match @Mixin
                        var targets = (List<AnnotationValue>) annotationMirror.getElementValues()
                                .entrySet()
                                .stream()
                                .filter(it -> it.getKey().getSimpleName().contentEquals("value"))
                                .findFirst()
                                .get().getValue().getValue();
                        var set = new HashSet<TypeElement>();
                        targets.getFirst().accept(annotationVisitor, set);
                        names.put(type.getQualifiedName().toString(), set);
                    }
                }
            }
        }
        var filer = super.processingEnv.getFiler();
        var resource = filer.createResource(CLASS_OUTPUT, "", "META-INF/mixins.txt");
        try (var writer = resource.openWriter()) {
            for (var entry : names.entrySet()) {
                var mixin = entry.getKey();
                var targets = entry.getValue();
                writer.write("FOR " + mixin + "\n");
                for (TypeElement target : targets) {
                    writer.write("\t" + target.getQualifiedName().toString() + "\n");
                }
            }
        }
        return true;
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.annotationVisitor = new MixinAnnotationVisitor(processingEnv.getMessager());
    }
}

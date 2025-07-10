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

import java.util.*;
import java.util.function.Predicate;

public class AnnotationRuleParser {
    private final Map<String, String> aliases = new HashMap<>();
    private String currentAnnotationDescriptor = null;
    private final List<AnnotationEnhancer.EnhanceRule> rules = new ArrayList<>();

    private record SimpleEnhanceRule(
            AnnotationEnhancer.EnhanceType type,
            Predicate<String> name,
            String descriptor) implements AnnotationEnhancer.EnhanceRule {

        @Override
        public boolean shouldEnhance(AnnotationEnhancer.EnhanceType type, String descriptor) {
            // descriptor here is the descriptor of the target element
            return this.type == type && this.name.test(descriptor);
        }
    }

    public List<AnnotationEnhancer.EnhanceRule> parse(String input) {
        rules.clear();
        aliases.clear();
        currentAnnotationDescriptor = null;

        for (String line : input.split("\n")) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (line.startsWith("@")) {
                // Store the annotation descriptor
                String annotationName = line.substring(1);
                currentAnnotationDescriptor = "L" + annotationName.replace('.', '/') + ";";
            } else {
                var ruleSplit = line.split(" ", 2);
                if (ruleSplit.length != 2) {
                    throw new IllegalArgumentException("Invalid annotation descriptor: " + line);
                }
                var type = parseType(ruleSplit[0]);
                Predicate<String> descriptor = ruleSplit[1]::equals;
                if (type == AnnotationEnhancer.EnhanceType.STARTS_WITH) {
                    descriptor = it -> it.startsWith(ruleSplit[1]);
                    rules.add(new SimpleEnhanceRule(AnnotationEnhancer.EnhanceType.CLASS, descriptor, Objects.requireNonNull(currentAnnotationDescriptor)));
                    rules.add(new SimpleEnhanceRule(AnnotationEnhancer.EnhanceType.METHOD, descriptor, currentAnnotationDescriptor));
                    rules.add(new SimpleEnhanceRule(AnnotationEnhancer.EnhanceType.FIELD, descriptor, currentAnnotationDescriptor));
                }else{
                    rules.add(new SimpleEnhanceRule(type, descriptor, Objects.requireNonNull(currentAnnotationDescriptor)));
                }
            }
        }

        return rules;
    }

    private AnnotationEnhancer.EnhanceType parseType(String type) {
        return switch (type.toUpperCase()) {
            case "TYPE" -> AnnotationEnhancer.EnhanceType.CLASS;
            case "METHOD" -> AnnotationEnhancer.EnhanceType.METHOD;
            case "FIELD" -> AnnotationEnhancer.EnhanceType.FIELD;
            case "STARTS_WITH" -> AnnotationEnhancer.EnhanceType.STARTS_WITH;
            default -> throw new IllegalArgumentException("Unknown type: " + type);
        };
    }
}
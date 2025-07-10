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
import io.ib67.edge.parser.AnnotationRuleParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestAnnotationRuleParser {
    @Test
    public void test() {
        var rule = """
                @java/lang/Override
                METHOD Lio/ib67/edge/enhance/EdgeClassEnhancer;addTransformer(Ljava/util/function/ToIntFunction;Ljava/util/function/Supplier;)Lio/ib67/edge/enhance/EdgeClassEnhancer;
                FIELD Lio/ib67/edge/enhance/EdgeClassEnhancer;transformers
                @java/lang/Override2
                TYPE io/ib67/edge/enhance/EdgeClassEnhancer
                """;
        var result = new AnnotationRuleParser().parse(rule);
        assertTrue(result.stream().anyMatch(it -> it.descriptor().equals("Ljava/lang/Override;")));
        assertTrue(result.stream().anyMatch(it -> it.descriptor().equals("Ljava/lang/Override2;")));
        assertTrue(result.stream().anyMatch(it -> it.shouldEnhance(AnnotationEnhancer.EnhanceType.METHOD, "Lio/ib67/edge/enhance/EdgeClassEnhancer;addTransformer(Ljava/util/function/ToIntFunction;Ljava/util/function/Supplier;)Lio/ib67/edge/enhance/EdgeClassEnhancer;")));
        assertTrue(result.stream().anyMatch(it -> it.shouldEnhance(AnnotationEnhancer.EnhanceType.FIELD, "Lio/ib67/edge/enhance/EdgeClassEnhancer;transformers")));
        assertTrue(result.stream().anyMatch(it -> it.shouldEnhance(AnnotationEnhancer.EnhanceType.CLASS, "io/ib67/edge/enhancer/EdgeClassEnhancer")));
    }
}

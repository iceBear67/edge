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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestAnnotationRuleParser {
    @Test
    public void test() {
        var rule = """
                import java/lang/Override as Override
                # dot syntax is allowed in imports.
                import java.lang.Override2 as Override2 
                import io/ib67/edge/enhance/EdgeClassEnhancer as EdgeClassEnhancer
                import java/util/function/ToIntFunction as ToIntFunction
                import java/util/function/Supplier as Supplier
                
                @Override
                METHOD EdgeClassEnhancer.addTransformer(ToIntFunction,Supplier)EdgeClassEnhancer
                FIELD EdgeClassEnhancer.transformers
                # equals to METHOD io/ib67/edge/enhance/EdgeClassEnhancer.addTransformer(Ljava/util/function/ToIntFunction;Ljava/util/function/Supplier;)Lio/ib67/edge/enhance/EdgeClassEnhancer;
                @Override2
                TYPE EdgeClassEnhancer
                """;
        var result = new AnnotationRuleParser().parse(rule);
        assertTrue(result.stream().anyMatch(it -> it.descriptor().equals("Ljava/lang/Override;")));
        assertTrue(result.stream().anyMatch(it -> it.descriptor().equals("Ljava/lang/Override2;")));
        assertTrue(result.stream().anyMatch(it -> it.shouldEnhance(AnnotationEnhancer.EnhanceType.METHOD, "io/ib67/edge/enhance/EdgeClassEnhancer.addTransformer(Ljava/util/function/ToIntFunction;Ljava/util/function/Supplier;)Lio/ib67/edge/enhance/EdgeClassEnhancer;")));
        assertTrue(result.stream().anyMatch(it -> it.shouldEnhance(AnnotationEnhancer.EnhanceType.FIELD, "io/ib67/edge/enhance/EdgeClassEnhancer.transformers")));
        assertTrue(result.stream().anyMatch(it -> it.shouldEnhance(AnnotationEnhancer.EnhanceType.CLASS, "io/ib67/edge/enhance/EdgeClassEnhancer")));
    }
}

package io.ib67.edge;

import io.ib67.edge.enhance.AnnotationEnhancer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestAnnotationRuleParser {
    @Test
    public void test() {
        var rule = """
                import java/lang/Override as Override
                import java/lang/Override2 as Override2
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

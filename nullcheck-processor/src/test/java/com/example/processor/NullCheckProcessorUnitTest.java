package com.example.processor;

import org.junit.jupiter.api.Test;

class NullCheckProcessorUnitTest {

    @Test
    void bananaConstructorHasAllNullChecks() throws Exception {
        String source = """
            package com.example.demo;
            import com.example.annotations.NullCheck;

            @NullCheck
            public class Banana {
                private final String color;
                private final String shape;

                public Banana(String color, String shape) {
                    this.color = color;
                    this.shape = shape;
                }
            }
            """;

        var result = ProcessorTestUtils.compileSource(source, new NullCheckProcessor());
        ProcessorTestUtils.assertAllFieldsCheckedAutomatically(result);
    }

    @Test
    void fruitRecordHasAllNullChecks() throws Exception {
        String source = """
            package com.example.demo;
            import com.example.annotations.NullCheck;

            @NullCheck
            public record Fruit(String name, String type) {}
            """;

        var result = ProcessorTestUtils.compileSource(source, new NullCheckProcessor());
        ProcessorTestUtils.assertAllFieldsCheckedAutomatically(result);
    }
}

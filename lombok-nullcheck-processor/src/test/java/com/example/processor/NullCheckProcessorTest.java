package com.example.processor;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;

import static com.google.testing.compile.CompilationSubject.assertThat;

class NullCheckProcessorTest {

    @Test
    void processorInjectsNotesAndSucceeds() {
        JavaFileObject banana = JavaFileObjects.forSourceString("com.example.demo.Banana", """
            package com.example.demo;
            import com.example.annotations.NullCheck;
            import lombok.RequiredArgsConstructor;
            import org.springframework.util.Assert;

            @NullCheck
            @RequiredArgsConstructor
            public class Banana {
                private final String color;
            }
        """);

        Compilation compilation = Compiler.javac()
                .withProcessors(new NullCheckProcessor())
                .withOptions("--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
                             "--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
                             "--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
                             "--add-exports=jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED")
                .compile(banana);

        assertThat(compilation).succeeded();
        // Ensure our processor printed notes about injection
        assertThat(compilation).hadNoteContaining("Processing @NullCheck for: Banana");
        assertThat(compilation).hadNoteContaining("Injected null-check for param: color");
    }
}

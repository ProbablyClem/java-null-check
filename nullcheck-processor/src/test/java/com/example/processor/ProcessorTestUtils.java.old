package com.example.processor;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;

import javax.annotation.processing.Processor;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.net.URI;
import java.util.*;

/**
 * Utility methods for testing annotation processors in-memory.
 */
public class ProcessorTestUtils {

    public static CompilationResult compileSource(String javaSource, Processor processor) throws Exception {

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        JavaFileObject file = new SimpleJavaFileObject(
                URI.create("string:///Test.java"), JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return javaSource;
            }
        };

        var diagnostics = new javax.tools.DiagnosticCollector<JavaFileObject>();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);

        // Get classpath - needed for annotation and Assert classes
        String classpath = System.getProperty("java.class.path");

        JavacTask task = (JavacTask) compiler.getTask(
                null, fileManager, diagnostics, List.of(
                        "-classpath", classpath,
                        "--add-exports", "jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
                        "--add-exports", "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
                        "--add-exports", "jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
                        "--add-exports", "jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
                        "--add-exports", "jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED"
                ),
                null,
                Collections.singletonList(file)
        );

        task.setProcessors(Collections.singletonList(processor));
        // Parse, analyze, and process
        task.analyze();  // This triggers annotation processing
        // Print all diagnostics
        for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
            System.out.println(d.getKind() + ": " + d.getMessage(null));
            System.out.println("Line " + d.getLineNumber() + " col " + d.getColumnNumber());
            System.out.println("Source: " + d.getSource());
        }
        // Get compilation units after processing
        Iterable<? extends CompilationUnitTree> compilationUnits = task.parse();
        return new CompilationResult(task, compilationUnits);
    }

    public static class CompilationResult {
        public final JavacTask task;
        public final Iterable<? extends CompilationUnitTree> compilationUnits;

        public CompilationResult(JavacTask task, Iterable<? extends CompilationUnitTree> compilationUnits) {
            this.task = task;
            this.compilationUnits = compilationUnits;
        }
    }

    /**
     * Returns a map: constructor -> list of Assert.notNull statements
     */
    public static Map<String, List<String>> getConstructorAssertStatements(Iterable<? extends CompilationUnitTree> compilationUnits) {
        Map<String, List<String>> constructorMap = new HashMap<>();

        for (CompilationUnitTree cu : compilationUnits) {
            new TreeScanner<Void, Void>() {
                @Override
                public Void visitClass(com.sun.source.tree.ClassTree classTree, Void unused) {
                    List<String> fieldNames = new ArrayList<>();
                    for (var member : classTree.getMembers()) {
                        if (member instanceof VariableTree field) {
                            fieldNames.add(field.getName().toString());
                        }
                    }

                    for (var member : classTree.getMembers()) {
                        if (member instanceof MethodTree method && method.getName().contentEquals("<init>")) {
                            List<String> asserts = new ArrayList<>();
                            method.getBody().getStatements().forEach(stmt -> {
                                String stmtStr = stmt.toString();
                                if (stmtStr.contains("Assert.notNull")) {
                                    asserts.add(stmtStr);
                                }
                            });
                            constructorMap.put(method.toString(), asserts);
                        }
                    }
                    return super.visitClass(classTree, unused);
                }
            }.scan(cu, null);
        }

        return constructorMap;
    }

    /**
     * Asserts that every field has an Assert.notNull in every constructor.
     */
    public static void assertAllFieldsCheckedInAllConstructors(Map<String, List<String>> constructorMap, String... fieldNames) {
        for (Map.Entry<String, List<String>> entry : constructorMap.entrySet()) {
            List<String> asserts = entry.getValue();
            for (String field : fieldNames) {
                boolean found = asserts.stream().anyMatch(s -> s.contains(field));
                if (!found) {
                    throw new AssertionError("Missing Assert.notNull for field '" + field + "' in constructor:\n" + entry.getKey());
                }
            }
        }
    }

    /**
     * Convenience method: automatically detects all fields in the class and checks each constructor.
     */
    public static void assertAllFieldsCheckedAutomatically(CompilationResult result) {
        for (CompilationUnitTree cu : result.compilationUnits) {
            new TreeScanner<Void, Void>() {
                @Override
                public Void visitClass(com.sun.source.tree.ClassTree classTree, Void unused) {
                    List<String> fieldNames = new ArrayList<>();
                    for (var member : classTree.getMembers()) {
                        if (member instanceof VariableTree field) {
                            fieldNames.add(field.getName().toString());
                        }
                    }

                    Map<String, List<String>> constructorMap = getConstructorAssertStatements(result.compilationUnits);
                    assertAllFieldsCheckedInAllConstructors(constructorMap, fieldNames.toArray(new String[0]));
                    return super.visitClass(classTree, unused);
                }
            }.scan(cu, null);
        }
    }
}

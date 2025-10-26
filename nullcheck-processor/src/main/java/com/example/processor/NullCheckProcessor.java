package com.example.processor;

import com.example.annotations.NullCheck;
import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.*;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.Set;

@SupportedAnnotationTypes("com.example.annotations.NullCheck")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class NullCheckProcessor extends AbstractProcessor {

    private Trees trees;
    private TreeMaker maker;
    private Names names;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        trees = Trees.instance(processingEnv);
        Context context = ((com.sun.tools.javac.processing.JavacProcessingEnvironment) processingEnv).getContext();
        maker = TreeMaker.instance(context);
        names = Names.instance(context);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        // Skip early rounds to let Lombok generate constructors first
        if (!roundEnv.processingOver()) {
            for (Element annotated : roundEnv.getElementsAnnotatedWith(NullCheck.class)) {
                JCClassDecl clazz = (JCClassDecl) trees.getTree(annotated);
                JCCompilationUnit cu = (JCCompilationUnit) trees.getPath(annotated).getCompilationUnit();

                try {
                    java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter("/tmp/processor-debug.log", true));
                    pw.println("=== Processing class: " + clazz.name + " ===");
                    pw.println("Package before: " + cu.getPackage());
                    pw.println("Package name: " + (cu.getPackage() != null ? cu.getPackage().packge : "null"));
                    pw.println("CU defs size: " + cu.defs.size());
                    pw.println("CU defs: " + cu.defs);
                    pw.flush();
                    pw.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // Process only existing constructors
                clazz.getMembers().stream()
                     .filter(m -> m instanceof JCMethodDecl md && md.getName().contentEquals("<init>"))
                     .map(m -> (JCMethodDecl) m)
                     .forEach(constructor -> prependNullChecks(clazz, constructor));

                try {
                    java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter("/tmp/processor-debug.log", true));
                    pw.println("Package after: " + cu.getPackage());
                    pw.println("CU defs size after: " + cu.defs.size());
                    pw.println("CU defs after: " + cu.defs);
                    pw.println();
                    pw.flush();
                    pw.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return true;
    }

    private void injectAssertImport(JCCompilationUnit cu) {
        boolean hasImport = cu.getImports().stream()
                              .anyMatch(i -> i.toString().contains("com.example.Assert"));
        if (!hasImport) {
            JCExpression importExpr = maker.Select(
                    maker.Select(
                            maker.Ident(names.fromString("com")),
                            names.fromString("example")
                    ),
                    names.fromString("Assert")
            );
            JCImport importTree = maker.Import((JCFieldAccess) importExpr, false);

            // Insert import after package declaration (if any) but before other imports/classes
            // cu.defs is a list: [package decl (optional), imports..., type decls...]
            // We need to insert after package but before other elements
            if (cu.defs.nonEmpty() && cu.defs.head instanceof JCPackageDecl) {
                // There's a package declaration - insert after it
                cu.defs = cu.defs.tail.prepend(importTree).prepend(cu.defs.head);
            } else {
                // No package declaration - just prepend
                cu.defs = cu.defs.prepend(importTree);
            }
        }
    }

    private void prependNullChecks(JCClassDecl clazz, JCMethodDecl constructor) {
        for (JCVariableDecl param : constructor.params) {
            // Use fully qualified name: com.example.Assert
            JCExpression assertClass = maker.Select(
                    maker.Select(
                            maker.Ident(names.fromString("com")),
                            names.fromString("example")
                    ),
                    names.fromString("Assert")
            );

            JCMethodInvocation call = maker.Apply(
                    List.nil(),
                    maker.Select(assertClass, names.fromString("notNull")),
                    List.of(
                            maker.Literal(clazz.name.toString() + "." + param.getName()),
                            maker.Ident(param.getName())
                    )
            );
            constructor.body.stats = constructor.body.stats.prepend(maker.Exec(call));
        }
    }
}

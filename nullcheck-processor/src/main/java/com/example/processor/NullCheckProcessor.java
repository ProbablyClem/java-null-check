package com.example.processor;

import com.example.annotations.NullCheck;
import com.sun.source.util.Trees;
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
        for (Element annotated : roundEnv.getElementsAnnotatedWith(NullCheck.class)) {
            JCClassDecl clazz = (JCClassDecl) trees.getTree(annotated);
            JCCompilationUnit cu = (JCCompilationUnit) clazz.getTree();

            // --- Inject import for com.example.Assert ---
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
                cu.defs = cu.defs.prepend(importTree);
            }

            // --- Process all constructors ---
            clazz.getMembers().stream()
                 .filter(m -> m instanceof JCMethodDecl md && md.getName().contentEquals("<init>"))
                 .map(m -> (JCMethodDecl) m)
                 .forEach(constructor -> addNullChecksToConstructor(clazz, constructor));
        }
        return true;
    }

    private void addNullChecksToConstructor(JCClassDecl clazz, JCMethodDecl constructor) {
        for (JCVariableDecl param : constructor.params) {
            JCExpression assertIdent = maker.Ident(names.fromString("Assert"));
            JCMethodInvocation call = maker.Apply(
                    List.nil(),
                    maker.Select(assertIdent, names.fromString("notNull")),
                    List.of(
                            maker.Literal(clazz.name.toString() + "." + param.getName()),
                            maker.Ident(param.getName())
                    )
            );
            constructor.body.stats = constructor.body.stats.prepend(maker.Exec(call));
        }
    }
}

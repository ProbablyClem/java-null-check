package com.example.processor;

import com.example.annotations.NullCheck;
import com.sun.source.util.Trees;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Names;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.Set;

@SupportedAnnotationTypes("com.example.annotations.NullCheck")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class NullCheckProcessor extends AbstractProcessor {
    private JavacTrees trees;
    private TreeMaker treeMaker;
    private Names names;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.trees = JavacTrees.instance(processingEnv);
        Context context = ((JavacProcessingEnvironment) processingEnv).getContext();
        this.treeMaker = TreeMaker.instance(context);
        this.names = Names.instance(context);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(NullCheck.class)) {
            JCTree tree = (JCTree) trees.getTree(element);
            if (tree == null) continue;

            tree.accept(new TreeTranslator() {
                @Override
                public void visitClassDef(JCTree.JCClassDecl classDecl) {
                    for (JCTree def : classDecl.defs) {
                        if (def instanceof JCTree.JCMethodDecl method && method.name.contentEquals("<init>")) {
                            List<JCTree.JCStatement> statements = method.body.stats;
                            for (JCTree.JCVariableDecl param : method.params) {
                                JCTree.JCExpression assertCall = treeMaker.Apply(
                                        List.nil(),
                                        treeMaker.Select(treeMaker.Ident(names.fromString("Assert")), names.fromString("notNull")),
                                        List.of(treeMaker.Literal(classDecl.name.toString() + "." + param.name.toString()), treeMaker.Ident(param.name))
                                );
                                JCTree.JCStatement stmt = treeMaker.Exec(assertCall);
                                statements = statements.prepend(stmt);
                            }
                            method.body.stats = statements;
                        }
                    }
                    super.visitClassDef(classDecl);
                }
            });
        }
        return false;
    }
}

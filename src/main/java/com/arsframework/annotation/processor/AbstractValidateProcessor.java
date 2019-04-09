package com.arsframework.annotation.processor;

import java.util.Set;
import java.lang.annotation.Annotation;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;

import com.sun.source.tree.Tree;
import com.sun.tools.javac.util.Names;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;

/**
 * 参数校验注解处理器抽象实现
 *
 * @author yongqiang.wu
 */
public abstract class AbstractValidateProcessor extends AbstractProcessor {
    protected Names names;
    protected Context context;
    protected TreeMaker maker;
    protected JavacTrees trees;

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest().compareTo(SourceVersion.RELEASE_8) > 0 ? SourceVersion.latest() : SourceVersion.RELEASE_8;
    }

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);
        this.trees = JavacTrees.instance(env);
        this.context = ((JavacProcessingEnvironment) env).getContext();
        this.names = Names.instance(this.context);
        this.maker = TreeMaker.instance(this.context);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
        for (String type : this.getSupportedAnnotationTypes()) {
            try {
                Class<? extends Annotation> annotation = (Class<? extends Annotation>) Class.forName(type);
                for (Element element : env.getElementsAnnotatedWith(annotation)) {
                    if (element.getKind() == ElementKind.ENUM || element.getKind() == ElementKind.CLASS) { // 枚举/类元素
                        for (JCTree def : ((JCTree.JCClassDecl) trees.getTree(element)).defs) {
                            if (def.getKind() == Tree.Kind.METHOD) {
                                this.buildValidateBlock(((JCTree.JCMethodDecl) def).sym);
                            }
                        }
                    } else if (element.getKind() == ElementKind.CONSTRUCTOR || element.getKind() == ElementKind.METHOD) { // 方法元素
                        if (((Symbol.MethodSymbol) element).owner.getAnnotation(annotation) == null) {
                            this.buildValidateBlock((Symbol.MethodSymbol) element);
                        }
                    } else if (element.getKind() == ElementKind.PARAMETER) { // 参数元素
                        if (((Symbol.VarSymbol) element).owner.getAnnotation(annotation) == null
                                && ((Symbol.VarSymbol) element).owner.owner.getAnnotation(annotation) == null) {
                            this.buildValidateBlock((Symbol.VarSymbol) element);
                        }
                    }
                }
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        return true;
    }

    /**
     * 构建参数校验处理代码块
     *
     * @param param 参数代码对象
     */
    protected void buildValidateBlock(Symbol.VarSymbol param) {
        JCTree.JCIf condition = this.buildValidateCondition(param);
        if (condition != null) {
            JCTree.JCMethodDecl tree = (JCTree.JCMethodDecl) trees.getTree(param.owner);
            tree.body.stats = tree.body.stats.prepend(condition);
        }
    }

    /**
     * 构建参数校验处理代码块
     *
     * @param method 方法代码对象
     */
    protected void buildValidateBlock(Symbol.MethodSymbol method) {
        if (!method.params.isEmpty()) {
            JCTree.JCMethodDecl tree = trees.getTree(method);
            for (Symbol.VarSymbol param : method.params) {
                JCTree.JCStatement condition = this.buildValidateCondition(param);
                if (condition != null) {
                    tree.body.stats = tree.body.stats.prepend(condition);
                }
            }
        }
    }

    /**
     * 构建语法树参数验证条件
     *
     * @param param 参数代码对象
     * @return 验证条件表达式对象
     */
    protected abstract JCTree.JCIf buildValidateCondition(Symbol.VarSymbol param);
}

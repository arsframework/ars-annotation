package com.arsframework.annotation.processor;

import java.util.Set;
import java.util.List;
import java.util.Arrays;
import java.util.Iterator;
import java.util.ArrayList;
import java.lang.annotation.Annotation;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.MirroredTypesException;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;

import com.sun.source.tree.Tree;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;

import com.arsframework.annotation.Ignore;

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
                    if (element.getKind() == ElementKind.ENUM || element.getKind() == ElementKind.CLASS
                            || element.getKind() == ElementKind.INTERFACE) { // 枚举/类/接口元素
                        for (JCTree def : ((JCTree.JCClassDecl) trees.getTree(element)).defs) {
                            if (def.getKind() == Tree.Kind.METHOD && ((JCTree.JCMethodDecl) def).body != null) {
                                this.buildValidateBlock(((JCTree.JCMethodDecl) def).sym, annotation);
                            }
                        }
                    } else if ((element.getKind() == ElementKind.CONSTRUCTOR || element.getKind() == ElementKind.METHOD)
                            && Validates.lookupAnnotation(((Symbol) element).owner, annotation) == null) { // 方法元素
                        this.buildValidateBlock((Symbol.MethodSymbol) element, annotation);
                    } else if (element.getKind() == ElementKind.PARAMETER
                            && Validates.lookupAnnotation(((Symbol) element).owner, annotation) == null) { // 参数元素
                        this.buildValidateBlock((Symbol.VarSymbol) element, annotation);
                    }
                }
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        return true;
    }

    /**
     * 判断代码逻辑是否为构造方法调用
     *
     * @param statement 代码逻辑对象
     * @return true/false
     */
    protected boolean isConstructorInvocation(JCTree.JCStatement statement) {
        if (statement instanceof JCTree.JCExpressionStatement) {
            JCTree.JCExpression expression = ((JCTree.JCExpressionStatement) statement).expr;
            if (expression instanceof JCTree.JCMethodInvocation
                    && ((JCTree.JCMethodInvocation) expression).meth.getKind() == Tree.Kind.IDENTIFIER) {
                Name name = ((JCTree.JCIdent) ((JCTree.JCMethodInvocation) expression).meth).name;
                return name == name.table.names._this || name == name.table.names._super;
            }
        }
        return false;
    }

    /**
     * 判断注解是否被忽略
     *
     * @param param      参数代码对象
     * @param annotation 注解类型
     * @return true/false
     */
    protected boolean isIgnoreAnnotation(Symbol.VarSymbol param, Class<? extends Annotation> annotation) {
        Ignore ignore;
        if (annotation != Ignore.class && (ignore = Validates.lookupAnnotation(param, Ignore.class)) != null) {
            try {
                Class<? extends Annotation>[] classes = ignore.value();
                if (classes.length == 0) {
                    return true;
                }
                for (Class<? extends Annotation> cls : classes) {
                    if (cls == annotation) {
                        return true;
                    }
                }
            } catch (MirroredTypesException e) {
                List<? extends TypeMirror> mirrors = e.getTypeMirrors();
                if (mirrors.isEmpty()) {
                    return true;
                }
                String name = annotation.getCanonicalName();
                for (TypeMirror mirror : mirrors) {
                    if (mirror.toString().equals(name)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 构建参数校验处理代码块
     *
     * @param param      参数代码对象
     * @param annotation 注解类型
     */
    protected void buildValidateBlock(Symbol.VarSymbol param, Class<? extends Annotation> annotation) {
        JCTree.JCStatement condition;
        if (!this.isIgnoreAnnotation(param, annotation) && (condition = this.buildValidateCondition(param, annotation)) != null) {
            this.appendValidateBlock((Symbol.MethodSymbol) param.owner, Arrays.asList(condition));
        }
    }

    /**
     * 构建参数校验处理代码块
     *
     * @param method     方法代码对象
     * @param annotation 注解类型
     */
    protected void buildValidateBlock(Symbol.MethodSymbol method, Class<? extends Annotation> annotation) {
        if (method != null && method.params != null && !method.params.isEmpty()) {
            List<JCTree.JCStatement> conditions = new ArrayList<>(method.params.size());
            for (Symbol.VarSymbol param : method.params) {
                JCTree.JCStatement condition;
                if (!this.isIgnoreAnnotation(param, annotation) && (condition = this.buildValidateCondition(param, annotation)) != null) {
                    conditions.add(condition);
                }
            }
            if (!conditions.isEmpty()) {
                this.appendValidateBlock(method, conditions);
            }
        }
    }

    /**
     * 添加参数校验代码块
     *
     * @param method     方法代码对象
     * @param conditions 校验代码逻辑列表
     */
    private void appendValidateBlock(Symbol.MethodSymbol method, List<JCTree.JCStatement> conditions) {
        if (conditions.isEmpty()) {
            return;
        }
        JCTree.JCBlock body = trees.getTree(method).body; // 获取方法对应的方法体
        if (method.isConstructor() && this.isConstructorInvocation(body.stats.head)) { // 如果方法体存在构造方法调用则将构造方法调用代码放在第一行
            ListBuffer stats = ListBuffer.of(body.stats.head);
            Iterator<JCTree.JCStatement> iterator = body.stats.iterator();
            iterator.next();
            for (JCTree.JCStatement condition : conditions) {
                stats.append(condition);
            }
            while (iterator.hasNext()) {
                stats.append(iterator.next());
            }
            body.stats = stats.toList();
        } else { // 将参数校验代码逻辑插入到方法体开始位置
            for (int i = conditions.size() - 1; i > -1; i--) {
                body.stats = body.stats.prepend(conditions.get(i));
            }
        }
    }

    /**
     * 构建语法树参数验证条件
     *
     * @param param      参数代码对象
     * @param annotation 注解类型
     * @return 验证条件表达式对象
     */
    protected abstract JCTree.JCIf buildValidateCondition(Symbol.VarSymbol param, Class<? extends Annotation> annotation);
}

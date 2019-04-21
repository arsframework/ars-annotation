package com.arsframework.annotation.processor;

import java.util.*;
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
                    if (element.getKind() == ElementKind.ENUM || element.getKind() == ElementKind.CLASS) { // 枚举/类元素
                        for (JCTree def : ((JCTree.JCClassDecl) trees.getTree(element)).defs) {
                            if (def.getKind() == Tree.Kind.METHOD) {
                                this.buildValidateBlock(((JCTree.JCMethodDecl) def).sym);
                            }
                        }
                    } else if ((element.getKind() == ElementKind.CONSTRUCTOR || element.getKind() == ElementKind.METHOD)
                            && Validates.lookupAnnotation(((Symbol) element).owner, annotation) == null) { // 方法元素
                        this.buildValidateBlock((Symbol.MethodSymbol) element);
                    } else if (element.getKind() == ElementKind.PARAMETER
                            && Validates.lookupAnnotation(((Symbol) element).owner, annotation) == null) { // 参数元素
                        this.buildValidateBlock((Symbol.VarSymbol) element);
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
     * @param annotation 注解对象
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
     * @param param 参数代码对象
     */
    protected void buildValidateBlock(Symbol.VarSymbol param) {
        JCTree.JCStatement condition = this.buildValidateCondition(param);
        if (condition != null) {
            Symbol.MethodSymbol method = (Symbol.MethodSymbol) param.owner;
            this.appendValidateBlock(method, method.params.indexOf(param), Arrays.asList(condition));
        }
    }

    /**
     * 构建参数校验处理代码块
     *
     * @param method 方法代码对象
     */
    protected void buildValidateBlock(Symbol.MethodSymbol method) {
        if (method != null && method.params != null && !method.params.isEmpty()) {
            List<JCTree.JCStatement> conditions = new ArrayList<>(method.params.size());
            for (Symbol.VarSymbol param : method.params) {
                JCTree.JCStatement condition = this.buildValidateCondition(param);
                if (condition != null) {
                    conditions.add(condition);
                }
            }
            if (!conditions.isEmpty()) {
                this.appendValidateBlock(method, 0, conditions);
            }
        }
    }

    /**
     * 添加参数校验代码块
     *
     * @param method     方法代码对象
     * @param index      添加代码块下标位置
     * @param conditions 校验代码逻辑列表
     */
    private void appendValidateBlock(Symbol.MethodSymbol method, int index, List<JCTree.JCStatement> conditions) {
        if (conditions.isEmpty()) {
            return;
        }

        JCTree.JCBlock body = trees.getTree(method).body; // 获取方法对应的方法体
        if (method.isConstructor() && this.isConstructorInvocation(body.stats.head)) { // 如果方法体存在构造方法调用则将校验代码插入位置后移1位
            index++;
        }

        // 重置方法体代码块
        if (index == 0) { // 将参数校验条件表达式添加到方法代码块最前面
            for (int i = conditions.size() - 1; i > -1; i--) {
                body.stats = body.stats.prepend(conditions.get(i));
            }
        } else { // 将参数校验代码块从指定下标位置开始插入
            ListBuffer stats = new ListBuffer();
            Iterator<JCTree.JCStatement> iterator = body.stats.iterator();
            while (index-- > 0 && iterator.hasNext()) {
                stats.append(iterator.next());
            }
            for (JCTree.JCStatement condition : conditions) {
                stats.append(condition);
            }
            while (iterator.hasNext()) {
                stats.append(iterator.next());
            }
            body.stats = stats.toList();
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

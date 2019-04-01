package com.arsframework.annotation;

import java.util.*;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;

import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Names;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;

/**
 * @author yongqiang.wu
 * @description 参数断言注解处理器
 * @date 2019-03-18 16:14
 */
public class AssertProcessor extends AbstractProcessor {
    private Names names;
    private Context context;
    private TreeMaker maker;
    private JavacTrees trees;

    /**
     * 重构断言处理代码块
     *
     * @param method 方法代码对象
     */
    private void rebuildAssertBlock(Symbol.MethodSymbol method) {
        ListBuffer<JCTree.JCStatement> statements = new ListBuffer<>();
        for (Symbol.VarSymbol param : method.params) {
            JCTree.JCStatement statement = this.buildValidateStatement(param);
            if (statement != null) {
                statements.append(statement);
            }
        }
        if (!statements.isEmpty()) {
            JCTree.JCMethodDecl tree = trees.getTree(method);
            tree.body.stats = statements.appendList(tree.body.stats).toList();
        }
    }

    /**
     * 重构断言处理代码块
     *
     * @param param 参数代码对象
     */
    private void rebuildAssertBlock(Symbol.VarSymbol param) {
        JCTree.JCStatement statement = this.buildValidateStatement(param);
        if (statement != null) {
            ListBuffer<JCTree.JCStatement> statements = new ListBuffer<>();
            statements.append(statement);
            JCTree.JCMethodDecl tree = (JCTree.JCMethodDecl) trees.getTree(param.owner);
            tree.body.stats = statements.appendList(tree.body.stats).toList();
        }
    }

    /**
     * 查找代码对应的断言注解
     * <p>
     * 注解查找顺序：参数 > 方法 -> 类
     *
     * @param symbol 代码对象
     * @return 断言注解对象
     */
    private Assert lookupAssertAnnotation(Symbol symbol) {
        Assert assertion;
        do {
            assertion = symbol.getAnnotation(Assert.class);
        } while (assertion == null && (symbol = symbol.owner) != null);
        return assertion;
    }

    /**
     * 判断类代码对象是否为指定的对象类型
     *
     * @param symbol 类代码对象
     * @param types  对象类型数组
     * @return true/false
     */
    private boolean isType(Symbol.ClassSymbol symbol, Class<?>... types) {
        String name = symbol.type.tsym.toString();
        for (Class<?> type : types) {
            if (name.equals(type.getCanonicalName())) {
                return true;
            }
        }
        List<Type> interfaces = symbol.getInterfaces();
        for (Type type : interfaces) {
            if (type.tsym != null && this.isType((Symbol.ClassSymbol) type.tsym, types)) {
                return true;
            }
        }
        Type superclass = symbol.getSuperclass();
        return superclass == null || superclass.tsym == null ? false : this.isType((Symbol.ClassSymbol) superclass.tsym, types);
    }

    /**
     * 构建断言异常表达式
     *
     * @param type    异常类型
     * @param message 异常消息
     * @return 语法树异常表达式对象
     */
    private JCTree.JCExpression buildExceptionExpression(String type, String message) {
        String[] parts = type.split("\\.");
        JCTree.JCFieldAccess access = null;
        JCTree.JCIdent id = maker.Ident(names.fromString(parts[0]));
        for (int i = 1; i < parts.length; i++) {
            access = maker.Select(access == null ? id : access, names.fromString(parts[i]));
        }
        return maker.NewClass(null, List.nil(), access == null ? id : access, List.of(maker.Literal(TypeTag.CLASS, message)), null);
    }

    /**
     * 构建参数非空白验证表达式
     *
     * @param param 参数代码对象
     * @return 语法树参数验证表达式对象
     */
    private JCTree.JCExpression buildNonemptyExpression(Symbol.VarSymbol param) {
        // 排除基本数据类型参数
        if (param.type.isPrimitive()) {
            return null;
        }

        // 针对数组类型参数的判断逻辑（param.length == 0）
        if (param.type.getKind() == TypeKind.ARRAY) {
            return maker.Binary(
                    JCTree.Tag.EQ,
                    maker.Select(maker.Ident(names.fromString(param.name.toString())), names.fromString("length")),
                    maker.Literal(TypeTag.INT, 0)
            );
        }

        // 针对字符串、字典、集合类型参数的判断逻辑（param.isEmpty()）
        if (this.isType((Symbol.ClassSymbol) param.type.tsym, String.class, Map.class, Collection.class)) {
            return maker.Apply(
                    List.nil(),
                    maker.Select(
                            maker.Ident(names.fromString(param.name.toString())),
                            names.fromString("isEmpty")
                    ),
                    List.nil()
            );
        }

        // 针对字符序列类型参数的判断逻辑（param.length() == 0）
        if (this.isType((Symbol.ClassSymbol) param.type.tsym, CharSequence.class)) {
            return maker.Binary(
                    JCTree.Tag.EQ,
                    maker.Apply(
                            List.nil(),
                            maker.Select(
                                    maker.Ident(names.fromString(param.name.toString())),
                                    names.fromString("length")
                            ),
                            List.nil()
                    ),
                    maker.Literal(TypeTag.INT, 0)
            );
        }
        return null;
    }

    /**
     * 构建语法树参数验证声明，如果nonnull、nonempty都为false则返回null
     *
     * @param param 参数代码对象
     * @return 语法树参数验证声明对象
     */
    private JCTree.JCStatement buildValidateStatement(Symbol.VarSymbol param) {
        if (param.type.isPrimitive()) { // 排除基本数据类型参数
            return null;
        }

        // 查找代码断言注解
        Assert assertion = this.lookupAssertAnnotation(param);
        boolean nonnull = assertion.nonnull();
        boolean nonempty = assertion.nonempty();
        if (!nonnull && !nonempty) {
            return null;
        }

        // 验证null
        JCTree.JCBinary binary = maker.Binary(
                nonnull ? JCTree.Tag.EQ : JCTree.Tag.NE,
                maker.Ident(names.fromString(param.name.toString())),
                maker.Literal(TypeTag.BOT, null));

        // 验证空白
        JCTree.JCExpression nonemptyExpression;
        if (nonempty && (nonemptyExpression = this.buildNonemptyExpression(param)) != null) {
            binary = maker.Binary(nonnull ? JCTree.Tag.OR : JCTree.Tag.AND, binary, nonemptyExpression);
        }

        // 返回判断逻辑对象
        String exception;
        try {
            exception = assertion.exception().getCanonicalName();
        } catch (MirroredTypeException e) {
            exception = e.getTypeMirror().toString();
        }
        String message = String.format(assertion.message(), param.name.toString());
        JCTree.JCExpression exceptionExpression = this.buildExceptionExpression(exception, message);
        return maker.If(maker.Parens(binary), maker.Block(0, List.of(maker.Throw(exceptionExpression))), null);
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
    public SourceVersion getSupportedSourceVersion() {
        if (SourceVersion.latest().compareTo(SourceVersion.RELEASE_8) > 0) {
            return SourceVersion.latest();
        } else {
            return SourceVersion.RELEASE_8;
        }
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new HashSet<>(1);
        types.add(Assert.class.getCanonicalName());
        return types;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
        for (Element element : env.getElementsAnnotatedWith(Assert.class)) {
            if (element.getKind() == ElementKind.ENUM || element.getKind() == ElementKind.CLASS) { // 枚举/类元素
                Assert.Scope scope = element.getAnnotation(Assert.class).scope();
                for (JCTree def : ((JCTree.JCClassDecl) trees.getTree(element)).defs) {
                    if (def.getKind() == Tree.Kind.METHOD) {
                        JCTree.JCMethodDecl method = (JCTree.JCMethodDecl) def;
                        if (scope == Assert.Scope.ALL
                                || (scope == Assert.Scope.METHOD && !method.sym.isConstructor())
                                || (scope == Assert.Scope.CONSTRUCTOR && method.sym.isConstructor())) {
                            this.rebuildAssertBlock(method.sym);
                        }
                    }
                }
            } else if (element.getKind() == ElementKind.CONSTRUCTOR || element.getKind() == ElementKind.METHOD) { // 方法元素
                Symbol.MethodSymbol method = (Symbol.MethodSymbol) element;
                if (method.owner.getAnnotation(Assert.class) == null && !method.params.isEmpty()) {
                    this.rebuildAssertBlock(method);
                }
            } else if (element.getKind() == ElementKind.PARAMETER) { // 参数元素
                Symbol.VarSymbol param = (Symbol.VarSymbol) element;
                if (param.owner.getAnnotation(Assert.class) == null) {
                    this.rebuildAssertBlock(param);
                }
            }
        }
        return true;
    }
}

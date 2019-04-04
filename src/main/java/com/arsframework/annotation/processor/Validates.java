package com.arsframework.annotation.processor;

import java.util.Map;
import java.util.Set;
import java.util.Collection;
import java.lang.annotation.Annotation;

import javax.lang.model.type.TypeKind;

import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Names;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;

/**
 * 参数校验注解处理工具类
 *
 * @author yongqiang.wu
 */
public abstract class Validates {
    /**
     * 判断类代码对象是否为指定的对象类型
     *
     * @param symbol 类代码对象
     * @param types  对象类型数组
     * @return true/false
     */
    public static boolean isType(Symbol.ClassSymbol symbol, Class<?>... types) {
        String name = symbol.type.tsym.toString();
        for (Class<?> type : types) {
            if (name.equals(type.getCanonicalName())) {
                return true;
            }
        }
        List<Type> interfaces = symbol.getInterfaces();
        for (Type type : interfaces) {
            if (type.tsym != null && isType((Symbol.ClassSymbol) type.tsym, types)) {
                return true;
            }
        }
        Type superclass = symbol.getSuperclass();
        return superclass == null || superclass.tsym == null ? false : isType((Symbol.ClassSymbol) superclass.tsym, types);
    }

    /**
     * 判断代码类型是否是数字类型
     *
     * @param symbol 类代码
     * @return true/false
     */
    public static boolean isNumber(Symbol.ClassSymbol symbol) {
        return isType(symbol, byte.class, char.class, int.class, short.class, float.class, long.class, double.class,
                Character.class, Number.class);
    }

    /**
     * 安装参数、方法、类的顺序查找代码对应的注解
     *
     * @param symbol 代码对象
     * @param type   注解类型
     * @return 断言注解对象
     */
    public static <T extends Annotation> T lookupAnnotation(Symbol symbol, Class<T> type) {
        T annotation;
        do {
            annotation = symbol.getAnnotation(type);
        } while (annotation == null && (symbol = symbol.owner) != null);
        return annotation;
    }

    /**
     * 构建异常类对象
     *
     * @param maker   语法树构建器
     * @param names   语法树节点名称对象
     * @param type    异常类型
     * @param message 异常消息
     * @return 语法树类对象
     */
    public static JCTree.JCNewClass buildExceptionExpression(TreeMaker maker, Names names, String type, String message) {
        String[] parts = type.split("\\.");
        JCTree.JCFieldAccess access = null;
        JCTree.JCIdent id = maker.Ident(names.fromString(parts[0]));
        for (int i = 1; i < parts.length; i++) {
            access = maker.Select(access == null ? id : access, names.fromString(parts[i]));
        }
        return maker.NewClass(null, List.nil(), access == null ? id : access, List.of(maker.Literal(TypeTag.CLASS, message)), null);
    }

    /**
     * 构建非Null校验条件表达式
     *
     * @param maker 语法树构建器
     * @param names 语法树节点名称对象
     * @param param 参数代码对象
     * @return 表达式对象
     */
    public static JCTree.JCExpression buildNonnullExpression(TreeMaker maker, Names names, Symbol.VarSymbol param) {
        return param.type.isPrimitive() ? null :
                maker.Binary(JCTree.Tag.EQ, maker.Ident(names.fromString(param.name.toString())), maker.Literal(TypeTag.BOT, null));
    }

    /**
     * 构建参数非空验证表达式
     *
     * @param maker 语法树构建器
     * @param names 语法树节点名称对象
     * @param param 参数代码对象
     * @return 语法树参数验证表达式对象
     */
    public static JCTree.JCExpression buildNonemptyExpression(TreeMaker maker, Names names, Symbol.VarSymbol param) {
        if (param.type.isPrimitive()) {
            return null;
        }
        // 非Null验证表达式
        JCTree.JCExpression condition = buildNonnullExpression(maker, names, param);
        if (param.type.getKind() == TypeKind.ARRAY) { // 数组
            return maker.Binary(JCTree.Tag.OR, condition, maker.Binary(
                    JCTree.Tag.EQ,
                    maker.Select(maker.Ident(names.fromString(param.name.toString())), names.fromString("length")),
                    maker.Literal(TypeTag.INT, 0)
            ));
        } else if (isType((Symbol.ClassSymbol) param.type.tsym, String.class, Map.class, Set.class, Collection.class)) { // 字符串、字典、集合、列表
            return maker.Binary(JCTree.Tag.OR, condition, maker.Apply(
                    List.nil(),
                    maker.Select(
                            maker.Ident(names.fromString(param.name.toString())),
                            names.fromString("isEmpty")
                    ),
                    List.nil()
            ));
        } else if (isType((Symbol.ClassSymbol) param.type.tsym, CharSequence.class)) { // 字符序列
            return maker.Binary(JCTree.Tag.OR, condition, maker.Binary(
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
            ));
        }
        return condition;
    }

    /**
     * 构建参数格式验证表达式
     *
     * @param maker   语法树构建器
     * @param names   语法树节点名称对象
     * @param param   参数代码对象
     * @param pattern 格式模式
     * @return 语法树参数验证表达式对象
     */
    public static JCTree.JCExpression buildFormatExpression(TreeMaker maker, Names names, Symbol.VarSymbol param, String pattern) {
        // 构建条件判断依据
        JCTree.JCExpression basis = null;
        if (isType((Symbol.ClassSymbol) param.type.tsym, String.class)) {
            basis = maker.Ident(names.fromString(param.name.toString()));
        } else if (isNumber((Symbol.ClassSymbol) param.type.tsym) || isType((Symbol.ClassSymbol) param.type.tsym, CharSequence.class)) {
            if (param.type.isPrimitive()) {
                basis = maker.Apply(
                        List.nil(),
                        maker.Select(
                                maker.Ident(names.fromString("String")),
                                names.fromString("valueOf")
                        ),
                        List.of(maker.Ident(names.fromString(param.name.toString())))
                );
            } else {
                basis = maker.Apply(
                        List.nil(),
                        maker.Select(
                                maker.Ident(names.fromString(param.name.toString())),
                                names.fromString("toString")
                        ),
                        List.nil()
                );
            }
        }
        if (basis == null) {
            return null;
        }

        // 格式校验条件表达式
        JCTree.JCExpression condition = maker.Unary(
                JCTree.Tag.NOT,
                maker.Apply(
                        List.nil(),
                        maker.Select(basis, names.fromString("matches")),
                        List.of(maker.Literal(TypeTag.CLASS, pattern))
                )
        );
        return param.type.isPrimitive() ? condition : maker.Binary(
                JCTree.Tag.AND,
                maker.Binary(JCTree.Tag.NE, maker.Ident(names.fromString(param.name.toString())), maker.Literal(TypeTag.BOT, null)),
                condition
        );
    }

    /**
     * 构建比较依据表达式
     *
     * @param maker 语法树构建器
     * @param names 语法树节点名称对象
     * @param param 参数代码对象
     * @return 语法树比较依据表达式
     */
    private static JCTree.JCExpression buildCompareBasisExpression(TreeMaker maker, Names names, Symbol.VarSymbol param) {
        if (isNumber((Symbol.ClassSymbol) param.type.tsym)) { // 数字
            return maker.Ident(names.fromString(param.name.toString()));
        } else if (isType((Symbol.ClassSymbol) param.type.tsym, CharSequence.class)) { // 字符序列
            return maker.Apply(
                    List.nil(),
                    maker.Select(maker.Ident(names.fromString(param.name.toString())), names.fromString("length")),
                    List.nil()
            );
        } else if (param.type.getKind() == TypeKind.ARRAY) { // 数组
            return maker.Select(maker.Ident(names.fromString(param.name.toString())), names.fromString("length"));
        } else if (isType((Symbol.ClassSymbol) param.type.tsym, Map.class, Set.class, Collection.class)) { // 字典、集合、列表
            return maker.Apply(
                    List.nil(),
                    maker.Select(maker.Ident(names.fromString(param.name.toString())), names.fromString("size")),
                    List.nil()
            );
        }
        return null;
    }

    /**
     * 构建数字比较条件表达式
     *
     * @param maker 语法树构建器
     * @param names 语法树节点名称对象
     * @param param 参数代码对象
     * @param tag   比较标签
     * @param value 比较值
     * @return 语法树比较条件表达式
     */
    private static JCTree.JCExpression buildCompareConditionExpression(TreeMaker maker, Names names, Symbol.VarSymbol param,
                                                                       JCTree.Tag tag, long value) {
        // 构建条件判断依据
        JCTree.JCExpression basis = buildCompareBasisExpression(maker, names, param);
        if (basis == null) {
            return null;
        }

        // 构建校验条件表达式
        JCTree.JCExpression condition = maker.Binary(tag, basis, maker.Literal(TypeTag.LONG, value));
        return param.type.isPrimitive() ? condition : maker.Binary(
                JCTree.Tag.AND,
                maker.Binary(JCTree.Tag.NE, maker.Ident(names.fromString(param.name.toString())), maker.Literal(TypeTag.BOT, null)),
                condition
        );
    }

    /**
     * 构建参数最大值验证表达式
     *
     * @param maker 语法树构建器
     * @param names 语法树节点名称对象
     * @param param 参数代码对象
     * @param max   最大值
     * @return 语法树参数验证表达式对象
     */
    public static JCTree.JCExpression buildMaxExpression(TreeMaker maker, Names names, Symbol.VarSymbol param, long max) {
        return buildCompareConditionExpression(maker, names, param, JCTree.Tag.GT, max);
    }

    /**
     * 构建参数最小值验证表达式
     *
     * @param maker 语法树构建器
     * @param names 语法树节点名称对象
     * @param param 参数代码对象
     * @param min   最小值
     * @return 语法树参数验证表达式对象
     */
    public static JCTree.JCExpression buildMinExpression(TreeMaker maker, Names names, Symbol.VarSymbol param, long min) {
        return buildCompareConditionExpression(maker, names, param, JCTree.Tag.LT, min);
    }

    /**
     * 构建参数大小验证表达式
     *
     * @param maker 语法树构建器
     * @param names 语法树节点名称对象
     * @param param 参数代码对象
     * @param min   最小值
     * @param max   最大值
     * @return 语法树参数验证表达式对象
     */
    public static JCTree.JCExpression buildSizeExpression(TreeMaker maker, Names names, Symbol.VarSymbol param, long min, long max) {
        // 构建条件判断依据
        JCTree.JCExpression basis = buildCompareBasisExpression(maker, names, param);
        if (basis == null) {
            return null;
        }

        // 构建校验条件表达式
        JCTree.JCExpression condition = maker.Binary(
                JCTree.Tag.OR,
                maker.Binary(JCTree.Tag.LT, basis, maker.Literal(TypeTag.LONG, min)),
                maker.Binary(JCTree.Tag.GT, basis, maker.Literal(TypeTag.LONG, max))
        );
        return param.type.isPrimitive() ? condition : maker.Binary(
                JCTree.Tag.AND,
                maker.Binary(JCTree.Tag.NE, maker.Ident(names.fromString(param.name.toString())), maker.Literal(TypeTag.BOT, null)),
                condition
        );
    }
}
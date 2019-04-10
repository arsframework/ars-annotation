package com.arsframework.annotation.processor;

import java.util.Map;
import java.util.Date;
import java.util.Collection;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.lang.annotation.Annotation;

import javax.lang.model.type.TypeKind;
import javax.lang.model.type.MirroredTypeException;

import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Names;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.arsframework.annotation.Global;

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
        String name = symbol.toString();
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
                Byte.class, Character.class, Integer.class, Short.class, Float.class, Long.class, Double.class);
    }

    /**
     * 判断代码类型是否是可比较类型
     *
     * @param symbol 类代码
     * @return true/false
     */
    public static boolean isComparable(Symbol.ClassSymbol symbol) {
        return isNumber(symbol) || isType(symbol, Comparable.class);
    }

    /**
     * 安装参数、方法、类的顺序查找代码对应的注解
     *
     * @param symbol 代码对象
     * @param type   注解类型
     * @param <T>    注解类型
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
     * 查找可比较参数代码对象
     *
     * @param param 比较参数
     * @param arg   被比较参数名称
     * @return 参数代码对象
     */
    public static Symbol.VarSymbol lookupComparableArgument(Symbol.VarSymbol param, String arg) {
        if (param.name.toString().equals(arg) || !isComparable((Symbol.ClassSymbol) param.type.tsym)) {
            return null;
        }
        for (Symbol.VarSymbol var : ((Symbol.MethodSymbol) param.owner).params) {
            if (var.name.toString().equals(arg)) {
                if (!isComparable((Symbol.ClassSymbol) var.type.tsym)
                        || (!param.type.equals(var.type)
                        && (!isNumber((Symbol.ClassSymbol) param.type.tsym) || !isNumber((Symbol.ClassSymbol) var.type.tsym)))) {
                    return null;
                }
                return var;
            }
        }
        return null;
    }

    /**
     * 获取对象对应的代码类型标签
     *
     * @param object 对象
     * @return 代码类型标签
     */
    public static TypeTag getObjectType(Object object) {
        if (object == null) {
            return TypeTag.BOT;
        } else if (object instanceof Byte) {
            return TypeTag.BYTE;
        } else if (object instanceof Character) {
            return TypeTag.CHAR;
        } else if (object instanceof Integer) {
            return TypeTag.INT;
        } else if (object instanceof Long) {
            return TypeTag.LONG;
        } else if (object instanceof Short) {
            return TypeTag.SHORT;
        } else if (object instanceof Float) {
            return TypeTag.FLOAT;
        } else if (object instanceof Double) {
            return TypeTag.DOUBLE;
        } else if (object instanceof Boolean) {
            return TypeTag.BOOLEAN;
        }
        return TypeTag.CLASS;
    }

    /**
     * 将对象转换成参数代码表达式
     *
     * @param maker   语法树构建器
     * @param objects 对象数组
     * @return 代码表达式列表
     */
    public static List<JCTree.JCExpression> object2params(TreeMaker maker, Object... objects) {
        if (objects.length == 0) {
            return List.nil();
        } else if (objects.length == 1) {
            return List.of(maker.Literal(getObjectType(objects[0]), objects[0]));
        }
        ListBuffer<JCTree.JCExpression> arguments = new ListBuffer<>();
        for (Object object : objects) {
            if (object instanceof JCTree.JCExpression) {
                arguments.append((JCTree.JCExpression) object);
            } else {
                arguments.append(maker.Literal(getObjectType(object), object));
            }
        }
        return arguments.toList();
    }

    /**
     * 合并代码表达式
     *
     * @param maker       语法树构建器
     * @param tag         合并标签
     * @param expressions 代码表达式数组
     * @return 合并后代码表达式
     */
    public static JCTree.JCExpression merge(TreeMaker maker, JCTree.Tag tag, JCTree.JCExpression... expressions) {
        JCTree.JCExpression head = null;
        for (JCTree.JCExpression expression : expressions) {
            if (expression != null) {
                head = head == null ? expression : maker.Binary(tag, head, expression);
            }
        }
        return head;
    }

    /**
     * 构建类对象表达式
     *
     * @param maker 语法树构建器
     * @param names 语法树节点名称对象
     * @param type  类对象类型
     * @param args  类构造参数
     * @return 语法树类对象表达式
     */
    public static JCTree.JCNewClass buildClassExpression(TreeMaker maker, Names names, String type, Object... args) {
        String[] parts = type.split("\\.");
        JCTree.JCFieldAccess access = null;
        JCTree.JCIdent id = maker.Ident(names.fromString(parts[0]));
        for (int i = 1; i < parts.length; i++) {
            access = maker.Select(access == null ? id : access, names.fromString(parts[i]));
        }
        return maker.NewClass(null, List.nil(), access == null ? id : access, object2params(maker, args), null);
    }

    /**
     * 构建Null校验条件表达式
     *
     * @param maker 语法树构建器
     * @param names 语法树节点名称对象
     * @param param 参数代码对象
     * @return 表达式对象
     */
    public static JCTree.JCExpression buildNullExpression(TreeMaker maker, Names names, Symbol.VarSymbol param) {
        return param.type.isPrimitive() ? null : maker.Binary(
                JCTree.Tag.EQ,
                maker.Ident(names.fromString(param.name.toString())),
                maker.Literal(TypeTag.BOT, null)
        );
    }

    /**
     * 构建非Null校验条件表达式
     *
     * @param maker  语法树构建器
     * @param names  语法树节点名称对象
     * @param params 参数代码对象数组
     * @return 表达式对象
     */
    public static JCTree.JCExpression buildNonnullExpression(TreeMaker maker, Names names, Symbol.VarSymbol... params) {
        JCTree.JCExpression expression = null;
        for (Symbol.VarSymbol param : params) {
            if (!param.type.isPrimitive()) {
                JCTree.JCExpression condition = maker.Binary(
                        JCTree.Tag.NE,
                        maker.Ident(names.fromString(param.name.toString())),
                        maker.Literal(TypeTag.BOT, null)
                );
                expression = expression == null ? condition : maker.Binary(JCTree.Tag.AND, expression, condition);
            }
        }
        return expression;
    }

    /**
     * 构建参数空验证表达式
     *
     * @param maker 语法树构建器
     * @param names 语法树节点名称对象
     * @param param 参数代码对象
     * @param blank 针对字符串参数是否允许空白
     * @return 语法树参数验证表达式对象
     */
    public static JCTree.JCExpression buildEmptyExpression(TreeMaker maker, Names names, Symbol.VarSymbol param, boolean blank) {
        if (param.type.isPrimitive()) {
            return null;
        }
        // 非Null验证表达式
        JCTree.JCExpression condition = buildNullExpression(maker, names, param);
        if (param.type.getKind() == TypeKind.ARRAY) { // 数组
            return maker.Binary(JCTree.Tag.OR, condition, maker.Binary(
                    JCTree.Tag.EQ,
                    maker.Select(maker.Ident(names.fromString(param.name.toString())), names.fromString("length")),
                    maker.Literal(TypeTag.INT, 0)
            ));
        } else if (isType((Symbol.ClassSymbol) param.type.tsym, Map.class, Collection.class)) { // 字典、集合
            return maker.Binary(JCTree.Tag.OR, condition, maker.Apply(
                    List.nil(),
                    maker.Select(
                            maker.Ident(names.fromString(param.name.toString())),
                            names.fromString("isEmpty")
                    ),
                    List.nil()
            ));
        } else if (isType((Symbol.ClassSymbol) param.type.tsym, String.class)) { // 字符串
            return maker.Binary(JCTree.Tag.OR, condition, maker.Apply(
                    List.nil(),
                    maker.Select(
                            blank ? maker.Ident(names.fromString(param.name.toString())) : maker.Apply(
                                    List.nil(),
                                    maker.Select(
                                            maker.Ident(names.fromString(param.name.toString())),
                                            names.fromString("trim")
                                    ),
                                    List.nil()
                            ),
                            names.fromString("isEmpty")
                    ),
                    List.nil()
            ));
        } else if (isType((Symbol.ClassSymbol) param.type.tsym, CharSequence.class)) { // 字符序列
            return maker.Binary(JCTree.Tag.OR, condition,
                    blank ? maker.Binary(
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
                    ) : maker.Apply(
                            List.nil(),
                            maker.Select(
                                    maker.Apply(
                                            List.nil(),
                                            maker.Select(
                                                    maker.Apply(
                                                            List.nil(),
                                                            maker.Select(
                                                                    maker.Ident(names.fromString(param.name.toString())),
                                                                    names.fromString("toString")
                                                            ),
                                                            List.nil()
                                                    ),
                                                    names.fromString("trim")
                                            ),
                                            List.nil()
                                    ),
                                    names.fromString("isEmpty")
                            ),
                            List.nil()
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
        } else if (isNumber((Symbol.ClassSymbol) param.type.tsym)
                || isType((Symbol.ClassSymbol) param.type.tsym, Number.class, CharSequence.class)) {
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

        // 格式校验条件表达式
        return basis == null ? null : merge(maker, JCTree.Tag.AND, buildNonnullExpression(maker, names, param), maker.Unary(
                JCTree.Tag.NOT,
                maker.Apply(
                        List.nil(),
                        maker.Select(basis, names.fromString("matches")),
                        List.of(maker.Literal(TypeTag.CLASS, pattern))
                )
        ));
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
    public static JCTree.JCExpression buildNumberCompareExpression(TreeMaker maker, Names names,
                                                                   Symbol.VarSymbol param, JCTree.Tag tag, long value) {
        if (isNumber((Symbol.ClassSymbol) param.type.tsym)) { // 数字
            return maker.Binary(tag, maker.Ident(names.fromString(param.name.toString())), maker.Literal(TypeTag.LONG, value));
        } else if (isType((Symbol.ClassSymbol) param.type.tsym, Enum.class)) { // 枚举
            return maker.Binary(tag, maker.Apply(
                    List.nil(),
                    maker.Select(maker.Ident(names.fromString(param.name.toString())), names.fromString("ordinal")),
                    List.nil()
            ), maker.Literal(TypeTag.LONG, value));
        } else if (isType((Symbol.ClassSymbol) param.type.tsym, Date.class)) { // 日期
            return maker.Binary(tag, maker.Apply(
                    List.nil(),
                    maker.Select(maker.Ident(names.fromString(param.name.toString())), names.fromString("getTime")),
                    List.nil()
            ), maker.Literal(TypeTag.LONG, value));
        } else if (isType((Symbol.ClassSymbol) param.type.tsym, CharSequence.class)) { // 字符序列
            return maker.Binary(tag, maker.Apply(
                    List.nil(),
                    maker.Select(maker.Ident(names.fromString(param.name.toString())), names.fromString("length")),
                    List.nil()
            ), maker.Literal(TypeTag.LONG, value));
        } else if (param.type.getKind() == TypeKind.ARRAY) { // 数组
            return maker.Binary(tag,
                    maker.Select(maker.Ident(names.fromString(param.name.toString())), names.fromString("length")),
                    maker.Literal(TypeTag.LONG, value));
        } else if (isType((Symbol.ClassSymbol) param.type.tsym, Map.class, Collection.class)) { // 字典、集合
            return maker.Binary(tag, maker.Apply(
                    List.nil(),
                    maker.Select(maker.Ident(names.fromString(param.name.toString())), names.fromString("size")),
                    List.nil()
            ), maker.Literal(TypeTag.LONG, value));
        } else if (isType((Symbol.ClassSymbol) param.type.tsym, BigInteger.class)) { // 大整数
            return maker.Binary(tag, maker.Apply(
                    List.nil(),
                    maker.Select(maker.Ident(names.fromString(param.name.toString())), names.fromString("compareTo")),
                    List.of(buildClassExpression(maker, names, param.type.tsym.toString(), String.valueOf(value)))
            ), maker.Literal(TypeTag.INT, 0));
        } else if (isType((Symbol.ClassSymbol) param.type.tsym, BigDecimal.class)) { // 大小数
            return maker.Binary(tag, maker.Apply(
                    List.nil(),
                    maker.Select(maker.Ident(names.fromString(param.name.toString())), names.fromString("compareTo")),
                    List.of(buildClassExpression(maker, names, param.type.tsym.toString(), value))
            ), maker.Literal(TypeTag.INT, 0));
        }
        return null;
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
        return merge(maker, JCTree.Tag.AND,
                buildNonnullExpression(maker, names, param), buildNumberCompareExpression(maker, names, param, JCTree.Tag.GT, max));
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
        return merge(maker, JCTree.Tag.AND,
                buildNonnullExpression(maker, names, param), buildNumberCompareExpression(maker, names, param, JCTree.Tag.LT, min));
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
        return merge(maker, JCTree.Tag.AND, buildNonnullExpression(maker, names, param), merge(maker, JCTree.Tag.OR,
                buildNumberCompareExpression(maker, names, param, JCTree.Tag.LT, min),
                buildNumberCompareExpression(maker, names, param, JCTree.Tag.GT, max)
        ));
    }

    /**
     * 构建参数选项验证表达式
     *
     * @param maker   语法树构建器
     * @param names   语法树节点名称对象
     * @param param   参数代码对象
     * @param options 选项值数组
     * @return 语法树参数验证表达式对象
     */
    public static JCTree.JCExpression buildOptionExpression(TreeMaker maker, Names names, Symbol.VarSymbol param, long[] options) {
        if (options.length == 0) {
            return null;
        }
        JCTree.JCExpression condition = buildNumberCompareExpression(maker, names, param, JCTree.Tag.NE, options[0]);
        if (options.length > 1) {
            for (int i = 1; i < options.length; i++) {
                condition = maker.Binary(JCTree.Tag.AND, condition,
                        buildNumberCompareExpression(maker, names, param, JCTree.Tag.NE, options[i]));
            }
        }
        return merge(maker, JCTree.Tag.AND, buildNonnullExpression(maker, names, param), condition);
    }

    /**
     * 构建参数大于比较验证表达式
     *
     * @param maker 语法树构建器
     * @param names 语法树节点名称对象
     * @param param 参数代码对象
     * @param arg   被比较参数名称
     * @return 语法树参数验证表达式对象
     */
    public static JCTree.JCExpression buildGtExpression(TreeMaker maker, Names names, Symbol.VarSymbol param, String arg) {
        // 查找被比较参数
        Symbol.VarSymbol argument = lookupComparableArgument(param, arg);
        if (argument == null) {
            return null;
        }

        // 构建校验条件表达式
        JCTree.JCExpression condition;
        if (isType((Symbol.ClassSymbol) param.type.tsym, Comparable.class)) {
            condition = maker.Binary(
                    JCTree.Tag.LT,
                    maker.Apply(
                            List.nil(),
                            maker.Select(maker.Ident(names.fromString(param.name.toString())), names.fromString("compareTo")),
                            List.of(maker.Ident(names.fromString(arg)))
                    ),
                    maker.Literal(TypeTag.INT, 1)
            );
        } else {
            condition = maker.Binary(JCTree.Tag.LE,
                    maker.Ident(names.fromString(param.name.toString())), maker.Ident(names.fromString(arg)));
        }
        return merge(maker, JCTree.Tag.AND, buildNonnullExpression(maker, names, param, argument), condition);
    }

    /**
     * 构建参数大于等于比较验证表达式
     *
     * @param maker 语法树构建器
     * @param names 语法树节点名称对象
     * @param param 参数代码对象
     * @param arg   被比较参数名称
     * @return 语法树参数验证表达式对象
     */
    public static JCTree.JCExpression buildGeExpression(TreeMaker maker, Names names, Symbol.VarSymbol param, String arg) {
        // 查找被比较参数
        Symbol.VarSymbol argument = lookupComparableArgument(param, arg);
        if (argument == null) {
            return null;
        }

        // 构建校验条件表达式
        JCTree.JCExpression condition;
        if (isType((Symbol.ClassSymbol) param.type.tsym, Comparable.class)) {
            condition = maker.Binary(
                    JCTree.Tag.LT,
                    maker.Apply(
                            List.nil(),
                            maker.Select(maker.Ident(names.fromString(param.name.toString())), names.fromString("compareTo")),
                            List.of(maker.Ident(names.fromString(arg)))
                    ),
                    maker.Literal(TypeTag.INT, 0)
            );
        } else {
            condition = maker.Binary(JCTree.Tag.LT,
                    maker.Ident(names.fromString(param.name.toString())), maker.Ident(names.fromString(arg)));
        }
        return merge(maker, JCTree.Tag.AND, buildNonnullExpression(maker, names, param, argument), condition);
    }

    /**
     * 构建参数小于比较验证表达式
     *
     * @param maker 语法树构建器
     * @param names 语法树节点名称对象
     * @param param 参数代码对象
     * @param arg   被比较参数名称
     * @return 语法树参数验证表达式对象
     */
    public static JCTree.JCExpression buildLtExpression(TreeMaker maker, Names names, Symbol.VarSymbol param, String arg) {
        // 查找被比较参数
        Symbol.VarSymbol argument = lookupComparableArgument(param, arg);
        if (argument == null) {
            return null;
        }

        // 构建校验条件表达式
        JCTree.JCExpression condition;
        if (isType((Symbol.ClassSymbol) param.type.tsym, Comparable.class)) {
            condition = maker.Binary(
                    JCTree.Tag.GT,
                    maker.Apply(
                            List.nil(),
                            maker.Select(maker.Ident(names.fromString(param.name.toString())), names.fromString("compareTo")),
                            List.of(maker.Ident(names.fromString(arg)))
                    ),
                    maker.Literal(TypeTag.INT, -1)
            );
        } else {
            condition = maker.Binary(JCTree.Tag.GE,
                    maker.Ident(names.fromString(param.name.toString())), maker.Ident(names.fromString(arg)));
        }
        return merge(maker, JCTree.Tag.AND, buildNonnullExpression(maker, names, param, argument), condition);
    }

    /**
     * 构建参数小于等于比较验证表达式
     *
     * @param maker 语法树构建器
     * @param names 语法树节点名称对象
     * @param param 参数代码对象
     * @param arg   被比较参数名称
     * @return 语法树参数验证表达式对象
     */
    public static JCTree.JCExpression buildLeExpression(TreeMaker maker, Names names, Symbol.VarSymbol param, String arg) {
        // 查找被比较参数
        Symbol.VarSymbol argument = lookupComparableArgument(param, arg);
        if (argument == null) {
            return null;
        }

        // 构建校验条件表达式
        JCTree.JCExpression condition;
        if (isType((Symbol.ClassSymbol) param.type.tsym, Comparable.class)) {
            condition = maker.Binary(
                    JCTree.Tag.GT,
                    maker.Apply(
                            List.nil(),
                            maker.Select(maker.Ident(names.fromString(param.name.toString())), names.fromString("compareTo")),
                            List.of(maker.Ident(names.fromString(arg)))
                    ),
                    maker.Literal(TypeTag.INT, 0)
            );
        } else {
            condition = maker.Binary(JCTree.Tag.GT,
                    maker.Ident(names.fromString(param.name.toString())), maker.Ident(names.fromString(arg)));
        }
        return merge(maker, JCTree.Tag.AND, buildNonnullExpression(maker, names, param, argument), condition);
    }


    /**
     * 构建验证异常块
     *
     * @param maker     语法树构建器
     * @param names     语法树节点名称对象
     * @param param     参数代码对象
     * @param condition 校验条件表达式
     * @param exception 异常类名称
     * @param message   异常信息
     * @param args      异常信息参数
     * @return 校验条件判断代码对象
     */
    public static JCTree.JCIf buildValidateException(TreeMaker maker, Names names, Symbol.VarSymbol param,
                                                     JCTree.JCExpression condition, String exception, String message, Object... args) {
        if (condition == null) {
            return null;
        }
        Global global;
        if ((global = lookupAnnotation(param.owner, Global.class)) != null
                && IllegalArgumentException.class.getCanonicalName().equals(exception)) {
            try {
                exception = global.exception().getCanonicalName();
            } catch (MirroredTypeException e) {
                exception = e.getTypeMirror().toString();
            }
        }
        return maker.If(condition, maker.Throw(buildClassExpression(maker, names, exception, String.format(message, args))), null);
    }
}

package com.arsframework.annotation;

import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;

/**
 * 方法参数非空断言验证注解，该注解可作用于类、方法、参数，参数配置优先使用顺序：参数、方法、类
 *
 * @author yongqiang.wu
 * @see com.arsframework.annotation.Nonnull
 * @see com.arsframework.annotation.Nonempty
 */
@Deprecated
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.PARAMETER})
public @interface Assert {
    /**
     * 针对类注解的方法搜索范围枚举
     */
    enum Scope {
        /**
         * 所有方法
         */
        ALL,

        /**
         * 对象方法
         */
        METHOD,

        /**
         * 构造方法
         */
        CONSTRUCTOR;
    }

    /**
     * 方法搜索范围
     *
     * @return 范围枚举
     */
    Scope scope() default Scope.ALL;

    /**
     * 是否允许为Null
     *
     * @return true/false
     */
    boolean nonnull() default true;

    /**
     * 是否允许参数为空
     * <p>
     * 如果为false，则对空字符串、空数组、空集合、空字典做验证
     *
     * @return true/false
     */
    boolean nonempty() default false;

    /**
     * 参数验证失败消息，内部采用java.lang.String.format(message,参数名称)方法对消息格式化
     *
     * @return 消息字符串
     */
    String message() default "Argument '%s' must not be empty";

    /**
     * 参数验证失败异常类型
     *
     * @return 异常类型
     */
    Class<? extends Throwable> exception() default IllegalArgumentException.class;
}

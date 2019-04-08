package com.arsframework.annotation;

import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;

/**
 * 参数值大于比较注解，适用于数字、java.lang.Comparable类型参数
 *
 * @author yongqiang.wu
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.PARAMETER})
public @interface Gt {
    /**
     * 获取被比较参数名称
     *
     * @return 参数名称
     */
    String value();

    /**
     * 参数验证失败消息，格式化参数:比较参数名称、被比较参数名称
     *
     * @return 消息字符串
     */
    String message() default "The value of argument '%s' must be greater than argument '%s'";

    /**
     * 参数验证失败异常类型
     *
     * @return 异常类型
     */
    String exception() default Global.DEFAULT_ARGUMENT_EXCEPTION;
}

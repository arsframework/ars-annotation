package com.arsframework.annotation;

import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;

/**
 * 参数值选项注解，适用于数字类型参数
 *
 * @author yongqiang.wu
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.PARAMETER})
public @interface Option {
    /**
     * 获取参数选项
     *
     * @return 选项数组
     */
    long[] value();

    /**
     * 参数验证失败消息，格式化参数:参数名称、参数选项
     *
     * @return 消息字符串
     */
    String message() default "The value of argument '%s' must be in option %s";

    /**
     * 参数验证失败异常类型
     *
     * @return 异常类型
     */
    String exception() default "java.lang.IllegalArgumentException";
}

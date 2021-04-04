package com.arsframework.annotation;

import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;

/**
 * 参数值选项注解，适用于数字、枚举、日期类型参数
 *
 * @author yongqiang.wu
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.PARAMETER})
public @interface Option {
    /**
     * 默认异常信息
     */
    String DEFAULT_EXCEPTION_MESSAGE = "The value of argument '%s' must be in option %s";

    /**
     * 获取参数选项
     *
     * @return 选项数组
     */
    long[] value();

    /**
     * 参数验证失败消息，格式化参数:参数名称、参数选项数组字符串
     *
     * @return 消息字符串
     */
    String message() default DEFAULT_EXCEPTION_MESSAGE;

    /**
     * 参数验证失败异常类型
     *
     * @return 异常类型
     */
    Class<? extends Throwable> exception() default IllegalArgumentException.class;
}

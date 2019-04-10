package com.arsframework.annotation;

import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;

/**
 * 参数格式校验注解，适用于数字、字符串类型参数
 *
 * @author yongqiang.wu
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.PARAMETER})
public @interface Format {
    /**
     * 默认异常信息
     */
    String DEFAULT_EXCEPTION_MESSAGE = "The format of argument '%s' must be matched for '%s'";

    /**
     * 获取参数匹配格式（正则表达式）
     *
     * @return 匹配格式
     */
    String value();

    /**
     * 参数验证失败消息，格式化参数:参数名称、匹配模式
     *
     * @return 消息字符串
     */
    String message() default DEFAULT_EXCEPTION_MESSAGE;

    /**
     * 参数验证失败异常类型
     *
     * @return 异常类型
     */
    String exception() default Global.DEFAULT_ARGUMENT_EXCEPTION;
}

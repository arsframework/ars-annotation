package com.arsframework.annotation;

import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;

/**
 * 参数非空校验注解，适用于除基本数据类型以外的参数
 *
 * @author yongqiang.wu
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.PARAMETER})
public @interface Nonempty {
    /**
     * 参数验证失败消息，格式化参数:参数名称
     *
     * @return 消息字符串
     */
    String message() default "The value of argument '%s' must not be empty";

    /**
     * 参数验证失败异常类型
     *
     * @return 异常类型
     */
    String exception() default Global.DEFAULT_ARGUMENT_EXCEPTION;
}

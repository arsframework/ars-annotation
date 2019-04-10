package com.arsframework.annotation;

import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;

/**
 * 参数非空白校验注解，适用于字符串类型参数
 *
 * @author yongqiang.wu
 * @see com.arsframework.annotation.Nonempty
 */
@Deprecated
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.PARAMETER})
public @interface Nonblank {
    /**
     * 默认异常信息
     */
    String DEFAULT_EXCEPTION_MESSAGE = "The value of argument '%s' must not be blank";

    /**
     * 参数验证失败消息，格式化参数:参数名称
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

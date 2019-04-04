package com.arsframework.annotation;

import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;

/**
 * 参数最小值校验表达式
 *
 * @author yongqiang.wu
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.PARAMETER})
public @interface Min {
    /**
     * 获取参数最小值
     *
     * @return 最小值
     */
    long value();

    /**
     * 参数验证失败消息，格式化参数:参数名称、参数最小值
     *
     * @return 消息字符串
     */
    String message() default "The size of argument '%s' must be greater than or equal to %d";

    /**
     * 参数验证失败异常类型
     *
     * @return 异常类型
     */
    String exception() default "java.lang.IllegalArgumentException";
}

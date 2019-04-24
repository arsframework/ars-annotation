package com.arsframework.annotation;

import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;

/**
 * 参数大小校验注解，适用于数字、枚举、日期、字符序列、数组、字典、集合、列表类型参数
 *
 * @author yongqiang.wu
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.PARAMETER})
public @interface Size {
    /**
     * 默认异常信息
     */
    String DEFAULT_EXCEPTION_MESSAGE = "The value of argument '%s' must be in interval [%d, %d]";

    /**
     * 获取参数最小值
     *
     * @return 最小值
     */
    long min() default Long.MIN_VALUE;

    /**
     * 获取参数最大值
     *
     * @return 最大值
     */
    long max() default Long.MAX_VALUE;

    /**
     * 参数验证失败消息，格式化参数:参数名称、参数最小值、参数最大值
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

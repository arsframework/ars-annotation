package com.arsframework.annotation;

import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;

/**
 * 全局配置注解
 *
 * @author yongqiang.wu
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.METHOD})
public @interface Global {
    /**
     * 参数验证失败异常类型
     *
     * @return 异常类型
     */
    Class<? extends Throwable> exception() default IllegalArgumentException.class;
}

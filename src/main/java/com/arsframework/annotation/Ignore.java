package com.arsframework.annotation;

import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;

/**
 * 忽略参数校验注解
 *
 * @author yongqiang.wu
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.PARAMETER})
public @interface Ignore {
    /**
     * 忽略目标注解类型数组
     *
     * @return 注解类型数组
     */
    Class<? extends Annotation>[] value() default {};
}

package com.arsframework.annotation.processor;

import java.lang.annotation.Annotation;

import javax.lang.model.type.MirroredTypeException;
import javax.annotation.processing.SupportedAnnotationTypes;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.arsframework.annotation.Min;

/**
 * 参数最小值校验注解处理器
 *
 * @author yongqiang.wu
 */
@SupportedAnnotationTypes("com.arsframework.annotation.Min")
public class MinValidateProcessor extends AbstractValidateProcessor {
    /**
     * 获取异常类名称
     *
     * @param min 校验注解实例
     * @return 类名称
     */
    protected String getException(Min min) {
        try {
            return min.exception().getCanonicalName();
        } catch (MirroredTypeException e) {
            return e.getTypeMirror().toString();
        }
    }

    @Override
    protected JCTree.JCIf buildValidateCondition(Symbol.VarSymbol param, Class<? extends Annotation> annotation) {
        Min min = (Min) Validates.lookupAnnotation(param, annotation);
        JCTree.JCExpression condition = Validates.buildMinExpression(maker, names, param, min.value());
        return Validates.buildValidateException(maker, names, param, condition, this.getException(min), min.message(),
                param.name.toString(), min.value());
    }
}

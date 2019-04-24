package com.arsframework.annotation.processor;

import java.lang.annotation.Annotation;

import javax.lang.model.type.MirroredTypeException;
import javax.annotation.processing.SupportedAnnotationTypes;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.arsframework.annotation.Bool;

/**
 * 参数真/假值校验注解处理器
 *
 * @author yongqiang.wu
 */
@SupportedAnnotationTypes("com.arsframework.annotation.Bool")
public class BoolValidateProcessor extends AbstractValidateProcessor {
    /**
     * 获取异常类名称
     *
     * @param bool 校验注解实例
     * @return 类名称
     */
    protected String getException(Bool bool) {
        try {
            return bool.exception().getCanonicalName();
        } catch (MirroredTypeException e) {
            return e.getTypeMirror().toString();
        }
    }

    @Override
    protected JCTree.JCIf buildValidateCondition(Symbol.VarSymbol param, Class<? extends Annotation> annotation) {
        Bool bool = (Bool) Validates.lookupAnnotation(param, annotation);
        JCTree.JCExpression condition = Validates.buildBoolExpression(maker, names, param, bool.value());
        return Validates.buildValidateException(maker, names, param, condition, this.getException(bool), bool.message(),
                param.name.toString(), bool.value());
    }
}

package com.arsframework.annotation.processor;

import java.lang.annotation.Annotation;

import javax.lang.model.type.MirroredTypeException;
import javax.annotation.processing.SupportedAnnotationTypes;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.arsframework.annotation.Not;

/**
 * 参数非固定值校验注解处理器
 *
 * @author yongqiang.wu
 */
@SupportedAnnotationTypes("com.arsframework.annotation.Not")
public class NotValidateProcessor extends AbstractValidateProcessor {
    /**
     * 获取异常类名称
     *
     * @param not 校验注解实例
     * @return 类名称
     */
    protected String getException(Not not) {
        try {
            return not.exception().getCanonicalName();
        } catch (MirroredTypeException e) {
            return e.getTypeMirror().toString();
        }
    }

    @Override
    protected JCTree.JCIf buildValidateCondition(Symbol.VarSymbol param, Class<? extends Annotation> annotation) {
        Not not = (Not) Validates.lookupAnnotation(param, annotation);
        JCTree.JCExpression condition = Validates.buildNotExpression(maker, names, param, not.value());
        return Validates.buildValidateException(maker, names, param, condition, this.getException(not), not.message(),
                param.name.toString(), not.value());
    }
}

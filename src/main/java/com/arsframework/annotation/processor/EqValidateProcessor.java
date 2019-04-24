package com.arsframework.annotation.processor;

import java.lang.annotation.Annotation;

import javax.lang.model.type.MirroredTypeException;
import javax.annotation.processing.SupportedAnnotationTypes;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.arsframework.annotation.Eq;

/**
 * 参数等于校验注解处理器
 *
 * @author yongqiang.wu
 */
@SupportedAnnotationTypes("com.arsframework.annotation.Eq")
public class EqValidateProcessor extends AbstractValidateProcessor {
    /**
     * 获取异常类名称
     *
     * @param eq 校验注解实例
     * @return 类名称
     */
    protected String getException(Eq eq) {
        try {
            return eq.exception().getCanonicalName();
        } catch (MirroredTypeException e) {
            return e.getTypeMirror().toString();
        }
    }

    @Override
    protected JCTree.JCIf buildValidateCondition(Symbol.VarSymbol param, Class<? extends Annotation> annotation) {
        Eq eq = (Eq) Validates.lookupAnnotation(param, annotation);
        JCTree.JCExpression condition = Validates.buildEqExpression(maker, names, param, eq.value());
        return Validates.buildValidateException(maker, names, param, condition, this.getException(eq), eq.message(),
                param.name.toString(), eq.value());
    }
}

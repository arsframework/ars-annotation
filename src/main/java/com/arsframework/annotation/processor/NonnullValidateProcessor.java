package com.arsframework.annotation.processor;

import java.lang.annotation.Annotation;

import javax.lang.model.type.MirroredTypeException;
import javax.annotation.processing.SupportedAnnotationTypes;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.arsframework.annotation.Nonnull;

/**
 * 参数非Null校验注解处理器
 *
 * @author yongqiang.wu
 */
@SupportedAnnotationTypes("com.arsframework.annotation.Nonnull")
public class NonnullValidateProcessor extends AbstractValidateProcessor {
    /**
     * 获取异常类名称
     *
     * @param nonnull 校验注解实例
     * @return 类名称
     */
    protected String getException(Nonnull nonnull) {
        try {
            return nonnull.exception().getCanonicalName();
        } catch (MirroredTypeException e) {
            return e.getTypeMirror().toString();
        }
    }

    @Override
    protected JCTree.JCIf buildValidateCondition(Symbol.VarSymbol param, Class<? extends Annotation> annotation) {
        Nonnull nonnull = (Nonnull) Validates.lookupAnnotation(param, annotation);
        JCTree.JCExpression condition = Validates.buildNullExpression(maker, names, param);
        return Validates.buildValidateException(maker, names, param, condition, this.getException(nonnull), nonnull.message(),
                param.name.toString());
    }
}

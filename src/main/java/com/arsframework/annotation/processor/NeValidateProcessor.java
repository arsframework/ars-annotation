package com.arsframework.annotation.processor;

import java.lang.annotation.Annotation;

import javax.lang.model.type.MirroredTypeException;
import javax.annotation.processing.SupportedAnnotationTypes;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.arsframework.annotation.Ne;

/**
 * 参数不等于校验注解处理器
 *
 * @author yongqiang.wu
 */
@SupportedAnnotationTypes("com.arsframework.annotation.Ne")
public class NeValidateProcessor extends AbstractValidateProcessor {
    /**
     * 获取异常类名称
     *
     * @param ne 校验注解实例
     * @return 类名称
     */
    protected String getException(Ne ne) {
        try {
            return ne.exception().getCanonicalName();
        } catch (MirroredTypeException e) {
            return e.getTypeMirror().toString();
        }
    }

    @Override
    protected JCTree.JCIf buildValidateCondition(Symbol.VarSymbol param, Class<? extends Annotation> annotation) {
        Ne ne = (Ne) Validates.lookupAnnotation(param, annotation);
        JCTree.JCExpression condition = Validates.buildNeExpression(maker, names, param, ne.value());
        return Validates.buildValidateException(maker, names, param, condition, this.getException(ne), ne.message(),
                param.name.toString(), ne.value());
    }
}

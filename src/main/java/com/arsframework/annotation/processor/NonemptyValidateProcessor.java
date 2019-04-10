package com.arsframework.annotation.processor;

import javax.lang.model.type.MirroredTypeException;
import javax.annotation.processing.SupportedAnnotationTypes;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.arsframework.annotation.Nonempty;

/**
 * 参数非空校验注解处理器
 *
 * @author yongqiang.wu
 */
@SupportedAnnotationTypes("com.arsframework.annotation.Nonempty")
public class NonemptyValidateProcessor extends AbstractValidateProcessor {
    /**
     * 获取异常类名称
     *
     * @param nonempty 参数非空校验注解
     * @return 类名称
     */
    protected String getException(Nonempty nonempty) {
        try {
            return nonempty.exception().getCanonicalName();
        } catch (MirroredTypeException e) {
            return e.getTypeMirror().toString();
        }
    }

    @Override
    protected JCTree.JCIf buildValidateCondition(Symbol.VarSymbol param) {
        Nonempty nonempty = Validates.lookupAnnotation(param, Nonempty.class);
        JCTree.JCExpression condition = Validates.buildEmptyExpression(maker, names, param, nonempty.blank());
        return Validates.buildValidateException(maker, names, param, condition, this.getException(nonempty), nonempty.message(),
                param.name.toString());
    }
}

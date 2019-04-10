package com.arsframework.annotation.processor;

import javax.lang.model.type.MirroredTypeException;
import javax.annotation.processing.SupportedAnnotationTypes;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.arsframework.annotation.Gt;

/**
 * 参数大于校验注解处理器
 *
 * @author yongqiang.wu
 */
@SupportedAnnotationTypes("com.arsframework.annotation.Gt")
public class GtValidateProcessor extends AbstractValidateProcessor {
    /**
     * 获取异常类名称
     *
     * @param gt 参数大于校验注解
     * @return 类名称
     */
    protected String getException(Gt gt) {
        try {
            return gt.exception().getCanonicalName();
        } catch (MirroredTypeException e) {
            return e.getTypeMirror().toString();
        }
    }

    @Override
    protected JCTree.JCIf buildValidateCondition(Symbol.VarSymbol param) {
        Gt gt = Validates.lookupAnnotation(param, Gt.class);
        JCTree.JCExpression condition = Validates.buildGtExpression(maker, names, param, gt.value());
        return Validates.buildValidateException(maker, names, param, condition, this.getException(gt), gt.message(),
                param.name.toString(), gt.value());
    }
}

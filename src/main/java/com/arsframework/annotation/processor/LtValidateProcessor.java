package com.arsframework.annotation.processor;

import javax.lang.model.type.MirroredTypeException;
import javax.annotation.processing.SupportedAnnotationTypes;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.arsframework.annotation.Lt;

/**
 * 参数小于校验注解处理器
 *
 * @author yongqiang.wu
 */
@SupportedAnnotationTypes("com.arsframework.annotation.Lt")
public class LtValidateProcessor extends AbstractValidateProcessor {
    /**
     * 获取异常类名称
     *
     * @param lt 参数小于校验注解
     * @return 类名称
     */
    protected String getException(Lt lt) {
        try {
            return lt.exception().getCanonicalName();
        } catch (MirroredTypeException e) {
            return e.getTypeMirror().toString();
        }
    }

    @Override
    protected JCTree.JCIf buildValidateCondition(Symbol.VarSymbol param) {
        Lt lt = Validates.lookupAnnotation(param, Lt.class);
        JCTree.JCExpression condition = Validates.buildLtExpression(maker, names, param, lt.value());
        return Validates.buildValidateException(maker, names, param, condition, this.getException(lt), lt.message(),
                param.name.toString(), lt.value());
    }
}

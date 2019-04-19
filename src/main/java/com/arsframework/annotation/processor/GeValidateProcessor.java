package com.arsframework.annotation.processor;

import javax.lang.model.type.MirroredTypeException;
import javax.annotation.processing.SupportedAnnotationTypes;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.arsframework.annotation.Ge;

/**
 * 参数大于等于校验注解处理器
 *
 * @author yongqiang.wu
 */
@SupportedAnnotationTypes("com.arsframework.annotation.Ge")
public class GeValidateProcessor extends AbstractValidateProcessor {
    /**
     * 获取异常类名称
     *
     * @param ge 参数大于或等于校验注解
     * @return 类名称
     */
    protected String getException(Ge ge) {
        try {
            return ge.exception().getCanonicalName();
        } catch (MirroredTypeException e) {
            return e.getTypeMirror().toString();
        }
    }

    @Override
    protected JCTree.JCIf buildValidateCondition(Symbol.VarSymbol param) {
        if (this.isIgnoreAnnotation(param, Ge.class)) {
            return null;
        }
        Ge ge = Validates.lookupAnnotation(param, Ge.class);
        JCTree.JCExpression condition = Validates.buildGeExpression(maker, names, param, ge.value());
        return Validates.buildValidateException(maker, names, param, condition, this.getException(ge), ge.message(),
                param.name.toString(), ge.value());
    }
}

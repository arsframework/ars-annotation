package com.arsframework.annotation.processor;

import javax.lang.model.type.MirroredTypeException;
import javax.annotation.processing.SupportedAnnotationTypes;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.arsframework.annotation.Format;

/**
 * 参数格式校验注解处理器
 *
 * @author yongqiang.wu
 */
@SupportedAnnotationTypes("com.arsframework.annotation.Format")
public class FormatValidateProcessor extends AbstractValidateProcessor {
    /**
     * 获取异常类名称
     *
     * @param format 参数格式校验注解
     * @return 类名称
     */
    protected String getException(Format format) {
        try {
            return format.exception().getCanonicalName();
        } catch (MirroredTypeException e) {
            return e.getTypeMirror().toString();
        }
    }

    @Override
    protected JCTree.JCIf buildValidateCondition(Symbol.VarSymbol param) {
        Format format = Validates.lookupAnnotation(param, Format.class);
        JCTree.JCExpression condition = Validates.buildFormatExpression(maker, names, param, format.value());
        return Validates.buildValidateException(maker, names, param, condition, this.getException(format), format.message(),
                param.name.toString(), format.value());
    }
}

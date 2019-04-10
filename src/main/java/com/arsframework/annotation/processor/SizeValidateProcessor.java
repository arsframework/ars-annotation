package com.arsframework.annotation.processor;

import javax.lang.model.type.MirroredTypeException;
import javax.annotation.processing.SupportedAnnotationTypes;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.arsframework.annotation.Size;

/**
 * 参数值大小校验注解处理器
 *
 * @author yongqiang.wu
 */
@SupportedAnnotationTypes("com.arsframework.annotation.Size")
public class SizeValidateProcessor extends AbstractValidateProcessor {
    /**
     * 获取异常类名称
     *
     * @param size 参数范围校验注解
     * @return 类名称
     */
    protected String getException(Size size) {
        try {
            return size.exception().getCanonicalName();
        } catch (MirroredTypeException e) {
            return e.getTypeMirror().toString();
        }
    }

    @Override
    protected JCTree.JCIf buildValidateCondition(Symbol.VarSymbol param) {
        Size size = Validates.lookupAnnotation(param, Size.class);
        JCTree.JCExpression condition = Validates.buildSizeExpression(maker, names, param, size.min(), size.max());
        return Validates.buildValidateException(maker, names, param, condition, this.getException(size), size.message(),
                param.name.toString(), size.min(), size.max());
    }
}

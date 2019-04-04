package com.arsframework.annotation.processor;

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

    @Override
    protected JCTree.JCIf buildValidateCondition(Symbol.VarSymbol param) {
        Size size = Validates.lookupAnnotation(param, Size.class);
        JCTree.JCExpression expression = Validates.buildSizeExpression(maker, names, param, size.min(), size.max());
        return expression == null ? null : maker.If(expression,
                maker.Throw(Validates.buildExceptionExpression(maker, names, size.exception(),
                        String.format(size.message(), param.name.toString(), size.min(), size.max()))), null);
    }
}

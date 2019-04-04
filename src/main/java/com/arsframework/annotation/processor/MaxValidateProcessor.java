package com.arsframework.annotation.processor;

import javax.annotation.processing.SupportedAnnotationTypes;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.arsframework.annotation.Max;

/**
 * 参数最大值校验注解处理器
 *
 * @author yongqiang.wu
 */
@SupportedAnnotationTypes("com.arsframework.annotation.Max")
public class MaxValidateProcessor extends AbstractValidateProcessor {

    @Override
    protected JCTree.JCIf buildValidateCondition(Symbol.VarSymbol param) {
        Max max = Validates.lookupAnnotation(param, Max.class);
        JCTree.JCExpression expression = Validates.buildMaxExpression(maker, names, param, max.value());
        return expression == null ? null : maker.If(expression,
                maker.Throw(Validates.buildExceptionExpression(maker, names, max.exception(),
                        String.format(max.message(), param.name.toString(), max.value()))), null);
    }
}

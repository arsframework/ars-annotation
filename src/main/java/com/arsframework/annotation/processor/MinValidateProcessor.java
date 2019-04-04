package com.arsframework.annotation.processor;

import javax.annotation.processing.SupportedAnnotationTypes;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.arsframework.annotation.Min;

/**
 * 参数最小值校验注解处理器
 *
 * @author yongqiang.wu
 */
@SupportedAnnotationTypes("com.arsframework.annotation.Min")
public class MinValidateProcessor extends AbstractValidateProcessor {

    @Override
    protected JCTree.JCIf buildValidateCondition(Symbol.VarSymbol param) {
        Min min = Validates.lookupAnnotation(param, Min.class);
        JCTree.JCExpression expression = Validates.buildMinExpression(maker, names, param, min.value());
        return expression == null ? null : maker.If(expression,
                maker.Throw(Validates.buildExceptionExpression(maker, names, min.exception(),
                        String.format(min.message(), param.name.toString(), min.value()))), null);
    }
}

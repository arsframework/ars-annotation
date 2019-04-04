package com.arsframework.annotation.processor;

import javax.annotation.processing.SupportedAnnotationTypes;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.arsframework.annotation.Le;

/**
 * 参数小于等于校验注解处理器
 *
 * @author yongqiang.wu
 */
@SupportedAnnotationTypes("com.arsframework.annotation.Le")
public class LeValidateProcessor extends AbstractValidateProcessor {

    @Override
    protected JCTree.JCIf buildValidateCondition(Symbol.VarSymbol param) {
        Le le = Validates.lookupAnnotation(param, Le.class);
        JCTree.JCExpression expression = Validates.buildLeExpression(maker, names, param, le.value());
        return expression == null ? null : maker.If(expression,
                maker.Throw(Validates.buildExceptionExpression(maker, names, le.exception(),
                        String.format(le.message(), param.name.toString(), le.value()))), null);
    }
}

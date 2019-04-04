package com.arsframework.annotation.processor;

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

    @Override
    protected JCTree.JCIf buildValidateCondition(Symbol.VarSymbol param) {
        Nonempty nonempty = Validates.lookupAnnotation(param, Nonempty.class);
        JCTree.JCExpression expression = Validates.buildEmptyExpression(maker, names, param);
        return expression == null ? null : maker.If(expression, maker.Throw(
                Validates.buildExceptionExpression(maker, names, nonempty.exception(),
                        String.format(nonempty.message(), param.name.toString()))), null);
    }
}

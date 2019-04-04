package com.arsframework.annotation.processor;

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

    @Override
    protected JCTree.JCIf buildValidateCondition(Symbol.VarSymbol param) {
        Ge ge = Validates.lookupAnnotation(param, Ge.class);
        JCTree.JCExpression expression = Validates.buildGeExpression(maker, names, param, ge.value());
        return expression == null ? null : maker.If(expression,
                maker.Throw(Validates.buildExceptionExpression(maker, names, ge.exception(),
                        String.format(ge.message(), param.name.toString(), ge.value()))), null);
    }
}

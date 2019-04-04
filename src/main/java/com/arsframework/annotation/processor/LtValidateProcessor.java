package com.arsframework.annotation.processor;

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

    @Override
    protected JCTree.JCIf buildValidateCondition(Symbol.VarSymbol param) {
        Lt lt = Validates.lookupAnnotation(param, Lt.class);
        JCTree.JCExpression expression = Validates.buildLtExpression(maker, names, param, lt.value());
        return expression == null ? null : maker.If(expression,
                maker.Throw(Validates.buildExceptionExpression(maker, names, lt.exception(),
                        String.format(lt.message(), param.name.toString(), lt.value()))), null);
    }
}

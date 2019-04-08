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
        JCTree.JCExpression condition = Validates.buildEmptyExpression(maker, names, param);
        return Validates.buildValidateException(maker, names, param, condition, nonempty.exception(), nonempty.message(),
                param.name.toString());
    }
}

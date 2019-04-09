package com.arsframework.annotation.processor;

import javax.annotation.processing.SupportedAnnotationTypes;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.arsframework.annotation.Nonblank;

/**
 * 参数非空白校验注解处理器
 *
 * @author yongqiang.wu
 */
@Deprecated
@SupportedAnnotationTypes("com.arsframework.annotation.Nonblank")
public class NonblankValidateProcessor extends AbstractValidateProcessor {

    @Override
    protected JCTree.JCIf buildValidateCondition(Symbol.VarSymbol param) {
        Nonblank nonblank = Validates.lookupAnnotation(param, Nonblank.class);
        JCTree.JCExpression condition = Validates.buildBlankExpression(maker, names, param);
        return Validates.buildValidateException(maker, names, param, condition, nonblank.exception(), nonblank.message(),
                param.name.toString());
    }
}

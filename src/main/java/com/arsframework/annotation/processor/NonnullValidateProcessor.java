package com.arsframework.annotation.processor;

import javax.annotation.processing.SupportedAnnotationTypes;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.arsframework.annotation.Nonnull;

/**
 * 参数非Null校验注解处理器
 *
 * @author yongqiang.wu
 */
@SupportedAnnotationTypes("com.arsframework.annotation.Nonnull")
public class NonnullValidateProcessor extends AbstractValidateProcessor {

    @Override
    protected JCTree.JCIf buildValidateCondition(Symbol.VarSymbol param) {
        Nonnull nonnull = Validates.lookupAnnotation(param, Nonnull.class);
        JCTree.JCExpression condition = Validates.buildNullExpression(maker, names, param);
        return Validates.buildValidateException(maker, names, param, condition, nonnull.exception(), nonnull.message(),
                param.name.toString());
    }
}

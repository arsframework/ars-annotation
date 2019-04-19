package com.arsframework.annotation.processor;

import java.util.Arrays;

import javax.lang.model.type.MirroredTypeException;
import javax.annotation.processing.SupportedAnnotationTypes;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.arsframework.annotation.Option;

/**
 * 参数选项校验注解处理器
 *
 * @author yongqiang.wu
 */
@SupportedAnnotationTypes("com.arsframework.annotation.Option")
public class OptionValidateProcessor extends AbstractValidateProcessor {
    /**
     * 获取异常类名称
     *
     * @param option 参数选项校验注解
     * @return 类名称
     */
    protected String getException(Option option) {
        try {
            return option.exception().getCanonicalName();
        } catch (MirroredTypeException e) {
            return e.getTypeMirror().toString();
        }
    }

    @Override
    protected JCTree.JCIf buildValidateCondition(Symbol.VarSymbol param) {
        if (this.isIgnoreAnnotation(param, Option.class)) {
            return null;
        }
        Option option = Validates.lookupAnnotation(param, Option.class);
        JCTree.JCExpression condition = Validates.buildOptionExpression(maker, names, param, option.value());
        return Validates.buildValidateException(maker, names, param, condition, this.getException(option), option.message(),
                param.name.toString(), Arrays.toString(option.value()));
    }
}

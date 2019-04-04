package com.arsframework.annotation.processor;

import javax.lang.model.type.MirroredTypeException;
import javax.annotation.processing.SupportedAnnotationTypes;

import com.sun.tools.javac.util.List;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.TypeTag;
import com.arsframework.annotation.Assert;

/**
 * 参数断言注解处理器
 *
 * @author yongqiang.wu
 */
@Deprecated
@SupportedAnnotationTypes("com.arsframework.annotation.Assert")
public class AssertProcessor extends AbstractValidateProcessor {

    @Override
    protected JCTree.JCIf buildValidateCondition(Symbol.VarSymbol param) {
        if (param.type.isPrimitive()) { // 排除基本数据类型参数
            return null;
        }

        // 查找代码断言注解
        Assert annotation = Validates.lookupAnnotation(param, Assert.class);
        boolean nonnull = annotation.nonnull();
        boolean nonempty = annotation.nonempty();
        if (!nonnull && !nonempty) {
            return null;
        }

        // 验证null
        JCTree.JCBinary binary = maker.Binary(
                nonnull ? JCTree.Tag.EQ : JCTree.Tag.NE,
                maker.Ident(names.fromString(param.name.toString())),
                maker.Literal(TypeTag.BOT, null));

        // 验证空白
        JCTree.JCExpression nonemptyExpression;
        if (nonempty && (nonemptyExpression = Validates.buildNonemptyExpression(maker, names, param)) != null) {
            binary = maker.Binary(nonnull ? JCTree.Tag.OR : JCTree.Tag.AND, binary, nonemptyExpression);
        }

        // 返回判断逻辑对象
        String exception;
        try {
            exception = annotation.exception().getCanonicalName();
        } catch (MirroredTypeException e) {
            exception = e.getTypeMirror().toString();
        }
        String message = String.format(annotation.message(), param.name.toString());
        JCTree.JCExpression exceptionExpression = Validates.buildExceptionExpression(maker, names, exception, message);
        return maker.If(maker.Parens(binary), maker.Block(0, List.of(maker.Throw(exceptionExpression))), null);
    }
}

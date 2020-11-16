package org.xyattic.eventual.consistency.support.core.aop.support;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;

import java.lang.annotation.Annotation;

/**
 * @author wangxing
 * @create 2020/11/12
 */
public class AnnotationClassOrMethodPointcut implements Pointcut {

    private final MethodMatcher methodMatcher;


    public AnnotationClassOrMethodPointcut(Class<? extends Annotation> annotationType) {
        this.methodMatcher = new AnnotationClassOrMethodMatcher(annotationType);
    }

    @Override
    public ClassFilter getClassFilter() {
        return ClassFilter.TRUE;
    }

    @Override
    public MethodMatcher getMethodMatcher() {
        return methodMatcher;
    }

}
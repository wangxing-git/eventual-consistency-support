package org.xyattic.eventual.consistency.support.core.aop.support;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.support.annotation.AnnotationMethodMatcher;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * @author wangxing
 * @create 2020/11/12
 */
public class AnnotationClassOrMethodMatcher extends AnnotationMethodMatcher {

    private final ClassFilter classFilter;

    public AnnotationClassOrMethodMatcher(Class<? extends Annotation> annotationType) {
        super(annotationType, true);
        this.classFilter = clazz -> AnnotatedElementUtils.isAnnotated(clazz, annotationType);
    }

    @Override
    public boolean matches(Method method, Class<?> targetClass) {
        return super.matches(method, targetClass) || classFilter.matches(targetClass);
    }

}
package org.xyattic.eventual.consistency.support.core.aop.support;

import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractBeanFactoryPointcutAdvisor;
import org.springframework.aop.support.ComposablePointcut;

import java.lang.annotation.Annotation;
import java.util.Arrays;

/**
 * @author wangxing
 * @create 2020/11/13
 */
public class AnnotationClassOrMethodBeanFactoryPointcutAdvisor extends AbstractBeanFactoryPointcutAdvisor {

    private ComposablePointcut pointcut;

    @SafeVarargs
    public AnnotationClassOrMethodBeanFactoryPointcutAdvisor(Class<? extends Annotation>... annotationTypes) {
        Arrays.stream(annotationTypes)
                .map(AnnotationClassOrMethodPointcut::new)
                .forEach(classOrMethodPointcut -> {
                    if (pointcut == null) {
                        pointcut = new ComposablePointcut(classOrMethodPointcut);
                    } else {
                        pointcut.union(classOrMethodPointcut);
                    }
                });
        if (pointcut == null) {
            pointcut = new ComposablePointcut();
        }
    }

    public AnnotationClassOrMethodBeanFactoryPointcutAdvisor(AnnotationMethodInterceptor<?> annotationMethodInterceptor) {
        this(annotationMethodInterceptor.getAnnotationType());
        setAdvice(annotationMethodInterceptor);
        setOrder(annotationMethodInterceptor.getOrder());
    }

    @Override
    public Pointcut getPointcut() {
        return pointcut;
    }
}
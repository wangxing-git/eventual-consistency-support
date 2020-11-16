package org.xyattic.eventual.consistency.support.core.aop.support;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.aop.aspectj.MethodInvocationProceedingJoinPoint;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.xyattic.eventual.consistency.support.core.provider.PendingMessageContextHolder;

import java.lang.annotation.Annotation;
import java.util.Objects;

/**
 * @author wangxing
 * @create 2020/11/13
 */
public abstract class AnnotationMethodInterceptor<T extends Annotation> implements MethodInterceptor, Ordered {

    protected final Class<T> annotationType;

    public Class<T> getAnnotationType() {
        return annotationType;
    }

    public AnnotationMethodInterceptor() {
        ResolvableType[] resolvableTypes = ResolvableType.forClass(getClass())
                .as(AnnotationMethodInterceptor.class)
                .getGenerics();
        this.annotationType = (Class<T>) resolvableTypes[0].getRawClass();
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        try {
            if (!(invocation instanceof ProxyMethodInvocation)) {
                throw new IllegalStateException("MethodInvocation is not a Spring " +
                        "ProxyMethodInvocation: " + invocation);
            }
            ProxyMethodInvocation pmi = (ProxyMethodInvocation) invocation;
            ProceedingJoinPoint pjp = lazyGetProceedingJoinPoint(pmi);
            T annotation = getAnnotation(invocation);

            return doInvoke(annotation, pjp);
        } finally {
            PendingMessageContextHolder.clear();
        }
    }

    protected abstract Object doInvoke(T annotation, ProceedingJoinPoint pjp);

    /**
     * Return the ProceedingJoinPoint for the current invocation,
     * instantiating it lazily if it hasn't been bound to the thread already.
     *
     * @param rmi the current Spring AOP ReflectiveMethodInvocation,
     *            which we'll use for attribute binding
     * @return the ProceedingJoinPoint to make available to advice methods
     */
    protected ProceedingJoinPoint lazyGetProceedingJoinPoint(ProxyMethodInvocation rmi) {
        return new MethodInvocationProceedingJoinPoint(rmi);
    }

    protected T getAnnotation(MethodInvocation invocation) {
        T annotation =
                AnnotatedElementUtils.getMergedAnnotation(invocation.getMethod(), annotationType);
        if (annotation == null) {
            annotation =
                    AnnotatedElementUtils.getMergedAnnotation(invocation.getMethod().getDeclaringClass(), annotationType);
        }
        return Objects.requireNonNull(annotation, "cannot found @" + annotationType.getName() +
                " info");
    }

}
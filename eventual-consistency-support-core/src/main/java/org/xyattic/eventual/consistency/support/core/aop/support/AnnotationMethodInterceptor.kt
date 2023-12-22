package org.xyattic.eventual.consistency.support.core.aop.support

import org.aopalliance.intercept.MethodInterceptor
import org.aopalliance.intercept.MethodInvocation
import org.aspectj.lang.ProceedingJoinPoint
import org.springframework.aop.ProxyMethodInvocation
import org.springframework.aop.aspectj.MethodInvocationProceedingJoinPoint
import org.springframework.core.Ordered
import org.springframework.core.ResolvableType
import org.springframework.core.annotation.AnnotatedElementUtils

/**
 * @author wangxing
 * @create 2020/11/13
 */
abstract class AnnotationMethodInterceptor<T : Annotation> : MethodInterceptor, Ordered {

    val annotationType: Class<T>

    override fun invoke(invocation: MethodInvocation): Any? {
        check(invocation is ProxyMethodInvocation) {
            "MethodInvocation is not a Spring " +
                    "ProxyMethodInvocation: " + invocation
        }
        val pjp = lazyGetProceedingJoinPoint(invocation)
        val annotation = getAnnotation(invocation)
        return doInvoke(annotation, pjp)
    }

    protected abstract fun doInvoke(annotation: T, pjp: ProceedingJoinPoint): Any?

    /**
     * Return the ProceedingJoinPoint for the current invocation,
     * instantiating it lazily if it hasn't been bound to the thread already.
     *
     * @param rmi the current Spring AOP ReflectiveMethodInvocation,
     * which we'll use for attribute binding
     * @return the ProceedingJoinPoint to make available to advice methods
     */
    protected fun lazyGetProceedingJoinPoint(rmi: ProxyMethodInvocation): ProceedingJoinPoint {
        return MethodInvocationProceedingJoinPoint(rmi)
    }

    protected fun getAnnotation(invocation: MethodInvocation): T {
        var annotation = AnnotatedElementUtils.getMergedAnnotation(invocation.method, annotationType)
        annotation = annotation
                ?: AnnotatedElementUtils.getMergedAnnotation(invocation.method.declaringClass, annotationType)
        check(annotation != null) {
            "cannot found @${annotationType.name} info"
        }
        return annotation
    }

    init {
        val resolvableTypes = ResolvableType.forClass(javaClass)
                .`as`(AnnotationMethodInterceptor::class.java)
                .generics
        annotationType = resolvableTypes[0].rawClass as Class<T>
    }

}
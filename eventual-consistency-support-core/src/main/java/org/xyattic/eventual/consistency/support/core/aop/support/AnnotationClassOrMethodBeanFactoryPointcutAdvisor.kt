package org.xyattic.eventual.consistency.support.core.aop.support

import org.springframework.aop.Pointcut
import org.springframework.aop.support.AbstractBeanFactoryPointcutAdvisor
import org.springframework.aop.support.ComposablePointcut
import java.util.*

/**
 * @author wangxing
 * @create 2020/11/13
 */
open class AnnotationClassOrMethodBeanFactoryPointcutAdvisor
@SafeVarargs constructor(vararg annotationTypes: Class<out Annotation>) :
        AbstractBeanFactoryPointcutAdvisor() {

    private var pointcut: ComposablePointcut

    constructor(annotationMethodInterceptor: AnnotationMethodInterceptor<*>) :
            this(annotationMethodInterceptor.annotationType) {
        advice = annotationMethodInterceptor
        order = annotationMethodInterceptor.order
    }

    override fun getPointcut(): Pointcut {
        return pointcut
    }

    init {
        var pointcut: ComposablePointcut? = null
        Arrays.stream(annotationTypes)
                .map { annotationType: Class<out Annotation> -> AnnotationClassOrMethodPointcut(annotationType) }
                .forEach { classOrMethodPointcut: AnnotationClassOrMethodPointcut ->
                    pointcut = pointcut?.union(classOrMethodPointcut)
                            ?: ComposablePointcut(classOrMethodPointcut)
                }
        this.pointcut = pointcut ?: ComposablePointcut()
    }

}
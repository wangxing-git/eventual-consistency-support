package org.xyattic.eventual.consistency.support.core.aop.support

import org.springframework.aop.ClassFilter
import org.springframework.aop.MethodMatcher
import org.springframework.aop.Pointcut

/**
 * @author wangxing
 * @create 2020/11/12
 */
open class AnnotationClassOrMethodPointcut(annotationType: Class<out Annotation>) : Pointcut {

    private val methodMatcher: MethodMatcher

    override fun getClassFilter(): ClassFilter {
        return ClassFilter.TRUE
    }

    override fun getMethodMatcher(): MethodMatcher {
        return methodMatcher
    }

    init {
        methodMatcher = AnnotationClassOrMethodMatcher(annotationType)
    }
}
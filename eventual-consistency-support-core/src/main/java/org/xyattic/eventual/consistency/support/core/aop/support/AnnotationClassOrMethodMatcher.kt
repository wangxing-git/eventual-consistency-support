package org.xyattic.eventual.consistency.support.core.aop.support

import org.springframework.aop.ClassFilter
import org.springframework.aop.support.annotation.AnnotationMethodMatcher
import org.springframework.core.annotation.AnnotatedElementUtils
import java.lang.reflect.Method

/**
 * @author wangxing
 * @create 2020/11/12
 */
open class AnnotationClassOrMethodMatcher(annotationType: Class<out Annotation>)
    : AnnotationMethodMatcher(annotationType, true) {

    private val classFilter: ClassFilter

    override fun matches(method: Method, targetClass: Class<*>): Boolean {
        return super.matches(method, targetClass) || classFilter.matches(targetClass)
    }

    init {
        classFilter = ClassFilter { clazz: Class<*> -> AnnotatedElementUtils.isAnnotated(clazz, annotationType) }
    }
}
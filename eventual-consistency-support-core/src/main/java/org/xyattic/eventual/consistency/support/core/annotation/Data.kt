package org.xyattic.eventual.consistency.support.core.annotation

import java.lang.annotation.Inherited

/**
 * @author wangxing
 * @create 2020/3/27
 */
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Inherited
internal annotation class Data
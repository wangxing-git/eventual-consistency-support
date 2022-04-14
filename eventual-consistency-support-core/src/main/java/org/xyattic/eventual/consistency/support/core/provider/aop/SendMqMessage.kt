package org.xyattic.eventual.consistency.support.core.provider.aop

import java.lang.annotation.Inherited

/**
 * @author wangxing
 * @create 2020/4/1
 */
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.CLASS
)
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Inherited
annotation class SendMqMessage(
    val persistenceName: String = "",
    val transactionManager: String = "",
    val delayedSend: Boolean = false
)
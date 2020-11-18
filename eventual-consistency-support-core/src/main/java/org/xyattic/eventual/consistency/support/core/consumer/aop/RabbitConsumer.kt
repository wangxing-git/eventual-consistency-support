package org.xyattic.eventual.consistency.support.core.consumer.aop

import java.lang.annotation.Inherited

/**
 * @author wangxing
 * @create 2020/3/27
 * use [ConsumeMqMessage]
 */
@Deprecated("use [ConsumeMqMessage]")
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.ANNOTATION_CLASS)
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Inherited
annotation class RabbitConsumer(val persistenceName: String = "", val transactionManager: String = "")
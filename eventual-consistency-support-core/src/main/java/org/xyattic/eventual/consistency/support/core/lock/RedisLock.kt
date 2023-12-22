package org.xyattic.eventual.consistency.support.core.lock

import java.lang.annotation.Inherited

/**
 * @author wangxing
 * @create 2020/4/10
 */
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention
@MustBeDocumented
@Inherited
annotation class RedisLock
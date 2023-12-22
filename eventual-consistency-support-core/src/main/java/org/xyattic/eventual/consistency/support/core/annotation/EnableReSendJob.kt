package org.xyattic.eventual.consistency.support.core.annotation

import org.springframework.context.annotation.Import
import org.xyattic.eventual.consistency.support.core.autoconfigure.SendPendingMessageJobConfiguration
import java.lang.annotation.Inherited

/**
 * @author wangxing
 * @create 2020/3/27
 */
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Inherited
@Import(SendPendingMessageJobConfiguration::class)
annotation class EnableReSendJob 
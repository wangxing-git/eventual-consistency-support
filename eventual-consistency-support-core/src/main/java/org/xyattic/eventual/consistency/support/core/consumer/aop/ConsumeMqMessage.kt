package org.xyattic.eventual.consistency.support.core.consumer.aop

import org.xyattic.eventual.consistency.support.core.consumer.AbstractMessage
import java.lang.annotation.Inherited
import kotlin.reflect.KClass

/**
 * @author wangxing
 */
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.ANNOTATION_CLASS
)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Inherited
annotation class ConsumeMqMessage(
    val persistenceName: String = "",
    val transactionManager: String = "",
    val messageClass: KClass<out AbstractMessage> = DefaultMessageClass::class,
    val messageExpression: String = "",
    val messageProvider: String = "",
    val messageIdExpression: String = ""
)

internal class DefaultMessageClass : AbstractMessage() {
    override fun getId(): String {
        TODO("Not yet implemented")
    }
}

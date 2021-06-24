package org.xyattic.eventual.consistency.support.core.consumer.aop

import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.exception.ExceptionUtils
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.reflect.MethodSignature
import org.reactivestreams.Publisher
import org.springframework.dao.DuplicateKeyException
import org.springframework.lang.Nullable
import org.springframework.transaction.reactive.TransactionalOperator
import org.xyattic.eventual.consistency.support.core.aop.support.AnnotationMethodInterceptor
import org.xyattic.eventual.consistency.support.core.consumer.ConsumedMessage
import org.xyattic.eventual.consistency.support.core.exception.DuplicateMessageException
import org.xyattic.eventual.consistency.support.core.exception.MqException
import org.xyattic.eventual.consistency.support.core.persistence.reactive.ReactiveConsumerPersistence
import org.xyattic.eventual.consistency.support.core.utils.SpelUtils
import org.xyattic.eventual.consistency.support.core.utils.SpringBeanUtils
import org.xyattic.eventual.consistency.support.core.utils.getLogger
import org.xyattic.eventual.consistency.support.core.utils.safeAs
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.io.Serializable
import java.util.*
import kotlin.reflect.jvm.kotlinFunction

/**
 * @author wangxing
 * @create 2020/8/24
 */
open class ReactiveConsumeMqMessageInterceptor : AnnotationMethodInterceptor<ConsumeMqMessage>() {

    companion object {
        private val log = getLogger()
    }

    override fun doInvoke(consumeMqMessage: ConsumeMqMessage, pjp: ProceedingJoinPoint): Any? {
        val message = parseMessage(pjp, consumeMqMessage)
        val id = parseMessageId(pjp, consumeMqMessage, message)

        val method = (pjp.signature as MethodSignature).method
        val isFlux: Boolean
        if (method.kotlinFunction?.isSuspend == true) {
            //suspend函数
            throw UnsupportedOperationException("Does not support suspend function")
        } else if (Publisher::class.java.isAssignableFrom(method.returnType)) {
            //reactive
            isFlux = Flux::class.java.isAssignableFrom(method.returnType)
        } else {
            throw UnsupportedOperationException("Only support Mono and Flux return types")
        }
        val consumedMessage = ConsumedMessage(id, message)
        val fallback: (DuplicateMessageException) -> Mono<Any> = {
            log.info("Duplicate message, ignored, id: " + id + ", message: " + message, it)
            Mono.empty()
        }
        return if (isFlux) {
            getTransactionalOperator(consumeMqMessage).transactional(
                saveSuccessConsumedMessages(
                    consumeMqMessage,
                    consumedMessage
                ).thenMany(pjp.proceed().safeAs<Flux<Any>>())
            ).onErrorResume(DuplicateMessageException::class.java, fallback)
        } else {
            getTransactionalOperator(consumeMqMessage).transactional(
                saveSuccessConsumedMessages(consumeMqMessage, consumedMessage).then(
                    pjp.proceed().safeAs<Mono<Any>>()
                )
            ).onErrorResume(DuplicateMessageException::class.java, fallback)
        }
    }

    protected open fun doInTransaction(
        pjp: ProceedingJoinPoint, consumeMqMessage: ConsumeMqMessage,
        id: Any, message: Serializable, isFlux: Boolean
    ): Publisher<Any> {
        val consumedMessage = ConsumedMessage(id, message)

        if (isFlux) {
            return saveSuccessConsumedMessages(
                consumeMqMessage,
                consumedMessage
            ).thenMany { pjp.proceed().safeAs() }
        } else {
            return saveSuccessConsumedMessages(consumeMqMessage, consumedMessage).then(
                pjp.proceed().safeAs()
            )
        }
    }

    private fun parseMessage(
        pjp: ProceedingJoinPoint,
        consumeMqMessage: ConsumeMqMessage
    ): Serializable {
        var message: Serializable? = null
        val messageClass = consumeMqMessage.messageClass
        if (DefaultMessageClass::class.java != messageClass) {
            message = findProvidedArgument(messageClass.java, *pjp.args)
        }
        if (StringUtils.isNotBlank(consumeMqMessage.messageExpression)) {
            val root: MutableMap<String?, Any?> = hashMapOf()
            root["args"] = pjp.args
            message = SpelUtils.parse(
                consumeMqMessage.messageExpression, root,
                Serializable::class.java
            )
        }
        if (StringUtils.isNotBlank(consumeMqMessage.messageProvider)) {
            val messageProvider = getMessageProvider(consumeMqMessage.messageProvider)
            message = messageProvider.getMessage(consumeMqMessage, pjp)
        }
        if (message == null) {
            throw MqException(
                "Unable to parse out the message! Please specify " +
                        "@ConsumeMqMessage property 'messageClass' or 'messageExpression' or " +
                        "'messageProvider' to help parse the message."
            )
        }
        return message
    }

    private fun parseMessageId(
        pjp: ProceedingJoinPoint, consumeMqMessage: ConsumeMqMessage,
        message: Serializable
    ): Any {
        val messageIdExpression: String = consumeMqMessage.messageIdExpression
        if (StringUtils.isBlank(messageIdExpression)) {
            throw MqException("messageIdExpression is blank")
        }
        val root: MutableMap<String?, Any?> = hashMapOf()
        root["args"] = pjp.args
        root["message"] = message
        return SpelUtils.parse<Any>(messageIdExpression, root)
            ?: throw MqException("cannot parse messageId with '${messageIdExpression}'")
    }

    protected fun saveSuccessConsumedMessages(
        consumeMqMessage: ConsumeMqMessage,
        consumedMessage: ConsumedMessage
    ): Mono<Void> {
        return saveConsumedMessages(consumeMqMessage, consumedMessage, true, null)
    }

    protected fun saveFailConsumedMessages(
        consumeMqMessage: ConsumeMqMessage,
        consumedMessage: ConsumedMessage,
        e: Exception?
    ): Mono<Void> {
        return saveConsumedMessages(consumeMqMessage, consumedMessage, false, e)
    }

    protected fun saveConsumedMessages(
        consumeMqMessage: ConsumeMqMessage,
        consumedMessage: ConsumedMessage,
        success: Boolean, e: Exception?
    ): Mono<Void> {
        consumedMessage.success = success
        if (e != null) {
            consumedMessage.exception = ExceptionUtils.getStackTrace(e)
        }
        consumedMessage.createTime = Date()
        consumedMessage.consumeTime = Date()
        return getReactiveConsumerPersistence(consumeMqMessage).save(consumedMessage)
            .onErrorResume(DuplicateKeyException::class.java) {
                Mono.error(DuplicateMessageException(it))
            }
    }

    protected fun getTransactionalOperator(consumeMqMessage: ConsumeMqMessage): TransactionalOperator {
        return SpringBeanUtils.getBean(
            consumeMqMessage.transactionManager,
            TransactionalOperator::class.java
        )
    }

    protected fun getMessageProvider(messageProvider: String?): MessageProvider<*> {
        return SpringBeanUtils.getBean(messageProvider, MessageProvider::class.java)
    }

    protected fun getReactiveConsumerPersistence(consumeMqMessage: ConsumeMqMessage): ReactiveConsumerPersistence {
        return SpringBeanUtils.getBean(
            consumeMqMessage.persistenceName,
            ReactiveConsumerPersistence::class.java
        )
    }

    override fun getOrder(): Int {
        return 10000
    }

    protected open fun <T> findProvidedArgument(
        parameter: Class<T>,
        @Nullable vararg providedArgs: Any?
    ): T? {
        return providedArgs.asSequence()
            .filter { parameter.isInstance(it) }
            .map { parameter.cast(it) }
            .firstOrNull()
    }
}
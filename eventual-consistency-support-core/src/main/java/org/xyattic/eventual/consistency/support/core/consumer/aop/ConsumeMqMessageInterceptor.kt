package org.xyattic.eventual.consistency.support.core.consumer.aop

import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.exception.ExceptionUtils
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.dao.DuplicateKeyException
import org.springframework.lang.Nullable
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionTemplate
import org.xyattic.eventual.consistency.support.core.aop.support.AnnotationMethodInterceptor
import org.xyattic.eventual.consistency.support.core.consumer.AbstractMessage
import org.xyattic.eventual.consistency.support.core.consumer.ConsumedMessage
import org.xyattic.eventual.consistency.support.core.exception.DuplicateMessageException
import org.xyattic.eventual.consistency.support.core.exception.EventualConsistencyException
import org.xyattic.eventual.consistency.support.core.persistence.ConsumerPersistence
import org.xyattic.eventual.consistency.support.core.provider.aop.SendMqMessage
import org.xyattic.eventual.consistency.support.core.utils.SpelUtils
import org.xyattic.eventual.consistency.support.core.utils.SpringBeanUtils
import org.xyattic.eventual.consistency.support.core.utils.TransactionUtils
import org.xyattic.eventual.consistency.support.core.utils.getLogger
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author wangxing
 * @create 2020/8/24
 */
open class ConsumeMqMessageInterceptor : AnnotationMethodInterceptor<ConsumeMqMessage>() {

    companion object {
        private val log = getLogger()

        fun doInTransaction(
                pjp: ProceedingJoinPoint, consumeMqMessage: ConsumeMqMessage,
                id: Any, message: AbstractMessage
        ): Any? {
            return try {
                val consumedMessage = ConsumedMessage(id, message)
                saveSuccessConsumedMessages(consumeMqMessage, consumedMessage)
                pjp.proceed()
            } catch (duplicateKeyException: DuplicateMessageException) {
                log.info(
                        "Duplicate message, ignored, id: " + id + ", message: " + message
                )
                null
            }
        }

        fun <T> findProvidedArgument(
                parameter: Class<T>,
                @Nullable vararg providedArgs: Any?
        ): T? {
            return providedArgs.asSequence()
                    .filter { parameter.isInstance(it) }
                    .map { parameter.cast(it) }
                    .firstOrNull()
        }

        fun parseMessage(
                pjp: ProceedingJoinPoint,
                consumeMqMessage: ConsumeMqMessage
        ): AbstractMessage {
            var message: AbstractMessage? = null
            val messageClass = consumeMqMessage.messageClass
            if (DefaultMessageClass::class.java != messageClass) {
                message = findProvidedArgument(messageClass.java, *pjp.args)
            }
            if (StringUtils.isNotBlank(consumeMqMessage.messageExpression)) {
                val root: MutableMap<String?, Any?> = hashMapOf()
                root["args"] = pjp.args
                message = SpelUtils.parse(
                        consumeMqMessage.messageExpression, root,
                        AbstractMessage::class.java
                )
            }
            if (StringUtils.isNotBlank(consumeMqMessage.messageProvider)) {
                val messageProvider = getMessageProvider(consumeMqMessage.messageProvider)
                message = messageProvider.getMessage(consumeMqMessage, pjp)
            }
            if (message == null) {
                throw EventualConsistencyException(
                        "Unable to parse out the message! Please specify " +
                                "@ConsumeMqMessage property 'messageClass' or 'messageExpression' or " +
                                "'messageProvider' to help parse the message."
                )
            }
            return message
        }

        fun parseMessageId(
                pjp: ProceedingJoinPoint, consumeMqMessage: ConsumeMqMessage,
                message: AbstractMessage
        ): Any {
            val messageIdExpression: String = consumeMqMessage.messageIdExpression
            if (StringUtils.isBlank(messageIdExpression)) {
                if (StringUtils.isBlank(message.getId())) {
                    throw EventualConsistencyException("messageIdExpression is blank and the message.getId() is blank")
                }
                return message.getId()
            }
            val root: MutableMap<String?, Any?> = hashMapOf()
            root["args"] = pjp.args
            root["message"] = message
            return SpelUtils.parse<Any>(messageIdExpression, root)
                    ?: throw EventualConsistencyException("cannot parse messageId with '${messageIdExpression}'")
        }

        fun saveSuccessConsumedMessages(
                consumeMqMessage: ConsumeMqMessage,
                consumedMessage: ConsumedMessage
        ) {
            saveConsumedMessages(consumeMqMessage, consumedMessage, true, null)
        }

        fun saveFailConsumedMessages(
                consumeMqMessage: ConsumeMqMessage,
                consumedMessage: ConsumedMessage,
                throwable: Throwable?
        ) {
            TransactionUtils.getTransactionTemplate(TransactionDefinition.PROPAGATION_REQUIRES_NEW, getPlatformTransactionManager(consumeMqMessage))
                    .execute {
                        try {
                            saveConsumedMessages(consumeMqMessage, consumedMessage, false, throwable)
                        } catch (duplicateKeyException: DuplicateMessageException) {
                            log.info(
                                    "Duplicate message, ignored, id: " + consumedMessage.id + ", message: " + consumedMessage
                            )
                        }
                    }
        }

        fun saveConsumedMessages(
                consumeMqMessage: ConsumeMqMessage,
                consumedMessage: ConsumedMessage,
                success: Boolean, throwable: Throwable?
        ) {
            try {
                consumedMessage.success = success
                if (throwable != null) {
                    consumedMessage.exception = ExceptionUtils.getStackTrace(throwable)
                }
                consumedMessage.createTime = Date()
                consumedMessage.consumeTime = Date()

                log.info("save consumed message: {}", consumedMessage)
                getConsumerPersistence(consumeMqMessage).save(consumedMessage)
            } catch (d: DuplicateKeyException) {
                throw DuplicateMessageException(d)
            }
        }

        fun getTransactionTemplate(consumeMqMessage: ConsumeMqMessage): TransactionTemplate {
            return SpringBeanUtils.getBean(
                    consumeMqMessage.transactionManager,
                    TransactionTemplate::class.java
            )
        }

        fun getMessageProvider(messageProvider: String?): MessageProvider<*> {
            return SpringBeanUtils.getBean(messageProvider, MessageProvider::class.java)
        }

        fun getConsumerPersistence(consumeMqMessage: ConsumeMqMessage): ConsumerPersistence {
            return SpringBeanUtils.getBean(
                    consumeMqMessage.persistenceName,
                    ConsumerPersistence::class.java
            )
        }

        fun getPlatformTransactionManager(consumeMqMessage: ConsumeMqMessage): PlatformTransactionManager {
            return SpringBeanUtils.getBean(
                    consumeMqMessage.transactionManager,
                    PlatformTransactionManager::class.java
            )
        }

    }

    override fun doInvoke(consumeMqMessage: ConsumeMqMessage, pjp: ProceedingJoinPoint): Any? {
        val methodSignature = pjp.signature as MethodSignature
        if (AnnotatedElementUtils.hasAnnotation(methodSignature.method, SendMqMessage::class.java)) {
            return pjp.proceed()
        }

        val message = parseMessage(pjp, consumeMqMessage)
        val id = parseMessageId(pjp, consumeMqMessage, message)
        val isNewTransaction = AtomicBoolean(false)
        try {
            if (message.reconsume.not()) {
                saveFailConsumedMessages(consumeMqMessage, ConsumedMessage(id, message), null)
                return null
            }
            return getTransactionTemplate(consumeMqMessage).execute { status: TransactionStatus ->
                isNewTransaction.set(status.isNewTransaction)
                doInTransaction(
                        pjp,
                        consumeMqMessage, id, message
                )
            }
        } catch (throwable: Throwable) {
            message.reconsumeTimes++
            if (isNewTransaction.get() && message.reconsume.not()) {
                saveFailConsumedMessages(consumeMqMessage, ConsumedMessage(id, message), throwable)
            }
            throw throwable;
        }
    }

    override fun getOrder(): Int {
        return 10000
    }

}
package org.xyattic.eventual.consistency.support.core.consumer.aop

import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.exception.ExceptionUtils
import org.aspectj.lang.ProceedingJoinPoint
import org.springframework.dao.DuplicateKeyException
import org.springframework.lang.Nullable
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionTemplate
import org.xyattic.eventual.consistency.support.core.aop.support.AnnotationMethodInterceptor
import org.xyattic.eventual.consistency.support.core.consumer.ConsumedMessage
import org.xyattic.eventual.consistency.support.core.exception.MqException
import org.xyattic.eventual.consistency.support.core.persistence.ConsumerPersistence
import org.xyattic.eventual.consistency.support.core.utils.SpelUtils
import org.xyattic.eventual.consistency.support.core.utils.SpringBeanUtils
import org.xyattic.eventual.consistency.support.core.utils.getLogger
import java.io.Serializable
import java.util.*

/**
 * @author wangxing
 * @create 2020/8/24
 */
open class ConsumeMqMessageInterceptor : AnnotationMethodInterceptor<ConsumeMqMessage>() {

    companion object {
        private val log = getLogger()
    }

    override fun doInvoke(consumeMqMessage: ConsumeMqMessage, pjp: ProceedingJoinPoint): Any? {
        val message = parseMessage(pjp, consumeMqMessage)
        val id = parseMessageId(pjp, consumeMqMessage, message)
        return try {
            getTransactionTemplate(consumeMqMessage)!!.execute { status: TransactionStatus? ->
                doInTransaction(pjp,
                        consumeMqMessage, id, message)
            }
        } catch (duplicateKeyException: DuplicateKeyException) {
            log.info("Duplicate message, ignored, id: {}, message: {}", id, message)
            null
        }
    }

    protected open fun doInTransaction(pjp: ProceedingJoinPoint, consumeMqMessage: ConsumeMqMessage,
                                       id: Any, message: Serializable): Any? {
        val consumedMessage = ConsumedMessage(id, message)
        saveSuccessConsumedMessages(consumeMqMessage, consumedMessage)
        return pjp.proceed()
    }

    private fun parseMessage(pjp: ProceedingJoinPoint, consumeMqMessage: ConsumeMqMessage): Serializable {
        var message: Serializable? = null
        val messageClass = consumeMqMessage.messageClass
        if (DefaultMessageClass::class.java != messageClass) {
            message = findProvidedArgument(messageClass.java, *pjp.args)
        }
        if (StringUtils.isNotBlank(consumeMqMessage.messageExpression)) {
            val root: MutableMap<String?, Any?> = hashMapOf()
            root["args"] = pjp.args
            message = SpelUtils.parse(consumeMqMessage.messageExpression, root,
                    Serializable::class.java)
        }
        if (StringUtils.isNotBlank(consumeMqMessage.messageProvider)) {
            val messageProvider = getMessageProvider(consumeMqMessage.messageProvider)
            message = messageProvider.getMessage(consumeMqMessage, pjp)
        }
        if (message == null) {
            throw MqException("unable to parse out the message! Please specify " +
                    "@ConsumeMqMessage property 'messageClass' or 'messageExpression' or " +
                    "'messageProvider' to help parse the message.")
        }
        return message
    }

    private fun parseMessageId(pjp: ProceedingJoinPoint, consumeMqMessage: ConsumeMqMessage,
                               message: Serializable): Any {
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

    protected fun saveSuccessConsumedMessages(consumeMqMessage: ConsumeMqMessage,
                                              consumedMessage: ConsumedMessage) {
        saveConsumedMessages(consumeMqMessage, consumedMessage, true, null)
    }

    protected fun saveFailConsumedMessages(consumeMqMessage: ConsumeMqMessage,
                                           consumedMessage: ConsumedMessage,
                                           e: Exception?) {
        saveConsumedMessages(consumeMqMessage, consumedMessage, false, e)
    }

    protected fun saveConsumedMessages(consumeMqMessage: ConsumeMqMessage,
                                       consumedMessage: ConsumedMessage,
                                       success: Boolean, e: Exception?) {
        consumedMessage.success = success
        if (e != null) {
            consumedMessage.exception = ExceptionUtils.getStackTrace(e)
        }
        consumedMessage.createTime = Date()
        consumedMessage.consumeTime = Date()
        getConsumerPersistence(consumeMqMessage).save(consumedMessage)
    }

    protected fun getTransactionTemplate(consumeMqMessage: ConsumeMqMessage): TransactionTemplate? {
        return SpringBeanUtils.getBean(consumeMqMessage.transactionManager,
                TransactionTemplate::class.java)
    }

    protected fun getMessageProvider(messageProvider: String?): MessageProvider<*> {
        return SpringBeanUtils.getBean(messageProvider, MessageProvider::class.java)
    }

    protected fun getConsumerPersistence(consumeMqMessage: ConsumeMqMessage): ConsumerPersistence {
        return SpringBeanUtils.getBean(consumeMqMessage.persistenceName,
                ConsumerPersistence::class.java)
    }

    override fun getOrder(): Int {
        return 10000
    }

    protected open fun <T> findProvidedArgument(parameter: Class<T>,
                                                @Nullable vararg providedArgs: Any?): T? {
        return providedArgs.asSequence()
                .filter { parameter.isInstance(it) }
                .map { parameter.cast(it) }
                .firstOrNull()
    }
}
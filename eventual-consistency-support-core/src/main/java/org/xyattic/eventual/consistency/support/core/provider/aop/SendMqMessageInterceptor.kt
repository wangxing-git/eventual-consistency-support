package org.xyattic.eventual.consistency.support.core.provider.aop

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.ResolvableType
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.dao.DuplicateKeyException
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.util.CollectionUtils
import org.xyattic.eventual.consistency.support.core.aop.support.AnnotationMethodInterceptor
import org.xyattic.eventual.consistency.support.core.consumer.ConsumedMessage
import org.xyattic.eventual.consistency.support.core.consumer.aop.ConsumeMqMessage
import org.xyattic.eventual.consistency.support.core.consumer.aop.ConsumeMqMessageInterceptor
import org.xyattic.eventual.consistency.support.core.exception.DuplicateMessageException
import org.xyattic.eventual.consistency.support.core.persistence.ProviderPersistence
import org.xyattic.eventual.consistency.support.core.provider.PendingMessage
import org.xyattic.eventual.consistency.support.core.provider.PendingMessageContextHolder
import org.xyattic.eventual.consistency.support.core.sender.Sender
import org.xyattic.eventual.consistency.support.core.utils.SpringBeanUtils
import org.xyattic.eventual.consistency.support.core.utils.getLogger
import java.lang.reflect.Method
import java.util.*
import java.util.stream.Collectors

/**
 * @author wangxing
 * @create 2020/6/25
 */
open class SendMqMessageInterceptor : AnnotationMethodInterceptor<SendMqMessage>() {

    companion object {
        private val log = getLogger()

        fun sendMessages(sender: Sender, pendingMessages: List<PendingMessage>) {
            try {
                sender.send(pendingMessages)
            } catch (e: Exception) {
                log.warn("send message abnormal: ${pendingMessages}", e)
            }
        }

        fun doInTransaction(
                pjp: ProceedingJoinPoint,
                sendMqMessage: SendMqMessage
        ): Any? {
            val result = pjp.proceed()
            savePendingMessages(
                    parseMessages(
                            result,
                            (pjp.signature as MethodSignature).method
                    ), sendMqMessage
            )
            return result
        }

        fun parseMessages(returnVal: Any?, method: Method?): List<PendingMessage> {
            if (returnVal is Collection<*>) {
                val clz = ResolvableType.forMethodReturnType(method)
                        .getGeneric(0).rawClass
                if (PendingMessage::class.java == clz) {
                    PendingMessageContextHolder.set(
                            (returnVal as Collection<PendingMessage>).stream()
                                    .collect(Collectors.toList())
                    )
                }
            } else if (returnVal is PendingMessage) {
                PendingMessageContextHolder.set(listOf(returnVal))
            }
            return PendingMessageContextHolder.get()
        }

        fun savePendingMessages(
                pendingMessages: List<PendingMessage>,
                sendMqMessage: SendMqMessage
        ) {
            if (!CollectionUtils.isEmpty(pendingMessages)) {
                pendingMessages.forEach { pendingMessage: PendingMessage ->
                    pendingMessage.persistenceName = sendMqMessage.persistenceName
                    pendingMessage.transactionManager = sendMqMessage.transactionManager
                    pendingMessage.createTime = Date()
                }
                pendingMessages.forEach {
                    try {
                        getProviderPersistence(sendMqMessage).save(it)
                    } catch (e: DuplicateKeyException) {
                        log.info("移除重复消息: ${it}")
                        PendingMessageContextHolder.remove(it)
                    }
                }
            }
        }

        fun getTransactionTemplate(sendMqMessage: SendMqMessage): TransactionTemplate {
            return SpringBeanUtils.getBean(
                    sendMqMessage.transactionManager,
                    TransactionTemplate::class.java
            )
        }

        fun getProviderPersistence(sendMqMessage: SendMqMessage): ProviderPersistence {
            return SpringBeanUtils.getBean(
                    sendMqMessage.persistenceName,
                    ProviderPersistence::class.java
            )
        }

    }

    @Autowired
    private lateinit var sender: Sender

    private val processed: ThreadLocal<Boolean> = ThreadLocal.withInitial { false }

    override fun doInvoke(sendMqMessage: SendMqMessage, pjp: ProceedingJoinPoint): Any? {
        if (processed.get()) {
            return pjp.proceed()
        }

        return try {
            processed.set(true)

            val methodSignature = pjp.signature as MethodSignature
            val consumeMqMessage = AnnotatedElementUtils.getMergedAnnotation(methodSignature.method, ConsumeMqMessage::class.java)
            if (consumeMqMessage != null) {
                return consumeAndSend(sendMqMessage, consumeMqMessage, pjp)
            } else {
                val result =
                        getTransactionTemplate(sendMqMessage).execute { status: TransactionStatus? ->
                            doInTransaction(pjp, sendMqMessage)
                        }
                if (sendMqMessage.delayedSend.not()) {
                    sendMessages(sender, PendingMessageContextHolder.get())
                }
                result
            }
        } finally {
            PendingMessageContextHolder.clear()
            processed.remove()
        }
    }

    protected open fun consumeAndSend(sendMqMessage: SendMqMessage, consumeMqMessage: ConsumeMqMessage, pjp: ProceedingJoinPoint): Any? {
        val message = ConsumeMqMessageInterceptor.parseMessage(pjp, consumeMqMessage)
        val id = ConsumeMqMessageInterceptor.parseMessageId(pjp, consumeMqMessage, message)

        val result = try {
            if (message.reconsume.not()) {
                ConsumeMqMessageInterceptor.saveFailConsumedMessages(consumeMqMessage, ConsumedMessage(id, message), null)
                return null
            }
            getTransactionTemplate(sendMqMessage).execute { status: TransactionStatus ->
                try {
                    val consumedMessage = ConsumedMessage(id, message)
                    ConsumeMqMessageInterceptor.saveSuccessConsumedMessages(consumeMqMessage, consumedMessage)
                    val result = pjp.proceed()
                    savePendingMessages(
                            parseMessages(
                                    result,
                                    (pjp.signature as MethodSignature).method
                            ), sendMqMessage
                    )
                    result
                } catch (duplicateMessageException: DuplicateMessageException) {
                    log.info(
                            "Duplicate message, ignored, id: " + id + ", message: " + message
                    )
                    null
                }
            }
        } catch (throwable: Throwable) {
            message.reconsumeTimes++
            if (message.reconsume.not()) {
                ConsumeMqMessageInterceptor.saveFailConsumedMessages(consumeMqMessage, ConsumedMessage(id, message), throwable)
            }
            throw throwable;
        }
        if (sendMqMessage.delayedSend.not()) {
            sendMessages(sender, PendingMessageContextHolder.get())
        }
        return result
    }

    override fun getOrder(): Int {
        return 0
    }

}
package org.xyattic.eventual.consistency.support.core.provider.aop

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.ResolvableType
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.util.CollectionUtils
import org.xyattic.eventual.consistency.support.core.aop.support.AnnotationMethodInterceptor
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
    }

    @Autowired
    private lateinit var sender: Sender

    override fun doInvoke(sendMqMessage: SendMqMessage, pjp: ProceedingJoinPoint): Any? {
        return try {
            val result = getTransactionTemplate(sendMqMessage).execute { status: TransactionStatus? ->
                doInTransaction(pjp, sendMqMessage)
            }
            sendMessages(PendingMessageContextHolder.get())
            result
        } finally {
            PendingMessageContextHolder.clear()
        }
    }

    protected open fun sendMessages(pendingMessages: List<PendingMessage>) {
        try {
            sender.send(pendingMessages)
        } catch (e: Exception) {
            log.warn("send message abnormal: ${pendingMessages}", e)
        }
    }

    protected open fun doInTransaction(pjp: ProceedingJoinPoint, sendMqMessage: SendMqMessage): Any? {
        val result = pjp.proceed()
        savePendingMessages(parseMessages(result,
                (pjp.signature as MethodSignature).method), sendMqMessage)
        return result
    }

    protected open fun parseMessages(returnVal: Any?, method: Method?): List<PendingMessage> {
        if (returnVal is Collection<*>) {
            val clz = ResolvableType.forMethodReturnType(method)
                    .getGeneric(0).rawClass
            if (PendingMessage::class.java == clz) {
                PendingMessageContextHolder.set((returnVal as Collection<PendingMessage>).stream()
                        .collect(Collectors.toList()))
            }
        } else if (returnVal is PendingMessage) {
            PendingMessageContextHolder.set(listOf(returnVal))
        }
        return PendingMessageContextHolder.get()
    }

    protected open fun savePendingMessages(pendingMessages: List<PendingMessage>,
                                           sendMqMessage: SendMqMessage) {
        if (!CollectionUtils.isEmpty(pendingMessages)) {
            pendingMessages.forEach { pendingMessage: PendingMessage ->
                pendingMessage.persistenceName = sendMqMessage.persistenceName
                pendingMessage.transactionManager = sendMqMessage.transactionManager
                pendingMessage.createTime = Date()
            }
            getProviderPersistence(sendMqMessage).save(pendingMessages)
        }
    }

    protected open fun getTransactionTemplate(sendMqMessage: SendMqMessage): TransactionTemplate {
        return SpringBeanUtils.getBean(sendMqMessage.transactionManager,
                TransactionTemplate::class.java)
    }

    protected open fun getProviderPersistence(sendMqMessage: SendMqMessage): ProviderPersistence {
        return SpringBeanUtils.getBean(sendMqMessage.persistenceName, ProviderPersistence::class.java)
    }

    override fun getOrder(): Int {
        return 0
    }

}
package org.xyattic.eventual.consistency.support.core.provider.aop

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.reflect.MethodSignature
import org.reactivestreams.Publisher
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.ResolvableType
import org.springframework.transaction.IllegalTransactionStateException
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.util.CollectionUtils
import org.xyattic.eventual.consistency.support.core.aop.support.AnnotationMethodInterceptor
import org.xyattic.eventual.consistency.support.core.persistence.reactive.ReactiveProviderPersistence
import org.xyattic.eventual.consistency.support.core.provider.PendingMessage
import org.xyattic.eventual.consistency.support.core.provider.ReactivePendingMessageContextHolder
import org.xyattic.eventual.consistency.support.core.sender.ReactiveSender
import org.xyattic.eventual.consistency.support.core.utils.SpringBeanUtils
import org.xyattic.eventual.consistency.support.core.utils.getLogger
import org.xyattic.eventual.consistency.support.core.utils.safeAs
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.context.Context
import java.lang.reflect.Method
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.function.BiFunction
import java.util.function.Function
import kotlin.reflect.jvm.kotlinFunction

/**
 * @author wangxing
 * @create 2020/6/25
 */
open class ReactiveSendMqMessageInterceptor : AnnotationMethodInterceptor<SendMqMessage>() {

    private val processedKey: String = "ReactiveSendMqMessageInterceptor.processed"

    companion object {
        private val log = getLogger()
    }

    @Autowired
    private lateinit var sender: ReactiveSender

    override fun doInvoke(sendMqMessage: SendMqMessage, pjp: ProceedingJoinPoint): Any? {
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
        val asyncError = BiFunction { _: String, t: Throwable -> Mono.empty<Void>() }
        val asyncCancel = Function<String, Mono<Void>> { Mono.empty() }
        val result = if (isFlux) {
            val targetFlux = pjp.proceed().safeAs<Flux<Any>>()
            Mono.subscriberContext()
                .flatMapMany {
                    if (it.hasKey(processedKey)) {
                        return@flatMapMany targetFlux
                    }
                    Flux.usingWhen(Mono.just(""), Function<String, Flux<Any>> {
                        getTransactionalOperator(sendMqMessage)
                            .transactional(
                                doInTransaction(
                                    pjp,
                                    sendMqMessage,
                                    targetFlux
                                ).safeAs<Flux<Any>>()
                            )
                            .onErrorResume(IllegalTransactionStateException::class.java) {
                                if (it.message.orEmpty()
                                        .startsWith("Transaction is already completed")
                                ) {
                                    log.info(it.message)
                                    return@onErrorResume Mono.empty()
                                }
                                Mono.error(it)
                            }
                    }, Function<String, Mono<Void>> {
                        sendMessages(sendMqMessage)
                    }, asyncError, asyncCancel).subscriberContext(
                        Context.of(
                            ReactivePendingMessageContextHolder.KEY,
                            CopyOnWriteArrayList<PendingMessage>(),
                            processedKey,
                            true
                        )
                    )
                }
        } else {
            val targetMono = pjp.proceed().safeAs<Mono<Any>>()
            Mono.subscriberContext()
                .flatMap {
                    if (it.hasKey(processedKey)) {
                        return@flatMap targetMono
                    }
                    Mono.usingWhen(Mono.just(""), Function<String, Mono<Any>> {
                        getTransactionalOperator(sendMqMessage)
                            .transactional(
                                doInTransaction(
                                    pjp,
                                    sendMqMessage,
                                    targetMono
                                ).safeAs<Mono<Any>>()
                            )
                            .onErrorResume(IllegalTransactionStateException::class.java) {
                                if (it.message.orEmpty()
                                        .startsWith("Transaction is already completed")
                                ) {
                                    log.info(it.message)
                                    return@onErrorResume Mono.empty()
                                }
                                Mono.error(it)
                            }
                    }, Function<String, Mono<Void>> {
                        sendMessages(sendMqMessage)
                    }, asyncError, asyncCancel).subscriberContext(
                        Context.of(
                            ReactivePendingMessageContextHolder.KEY,
                            CopyOnWriteArrayList<PendingMessage>(),
                            processedKey,
                            true
                        )
                    )
                }
        }
        return result
    }

    protected open fun sendMessages(sendMqMessage: SendMqMessage): Mono<Void> {
        if (sendMqMessage.delayedSend) {
            return Mono.empty()
        }
        return ReactivePendingMessageContextHolder.get()
            .flatMap { pendingMessages ->
                sender.send(pendingMessages)
                    .onErrorResume { e ->
                        log.warn("send message abnormal: ${pendingMessages}", e)
                        Mono.empty()
                    }
            }
    }

    protected open fun doInTransaction(
        pjp: ProceedingJoinPoint,
        sendMqMessage: SendMqMessage,
        isFlux: Boolean
    ): Publisher<Any> {
        val result = pjp.proceed()

        val savePendingMessages = parseMessages(
            result, (pjp.signature as MethodSignature).method
        ).flatMap { savePendingMessages(it, sendMqMessage) }
        return if (isFlux) {
            val cache = result.safeAs<Flux<Any>>().cache()
            cache.then(savePendingMessages).thenMany(cache)
        } else {
            val cache = result.safeAs<Mono<Any>>().cache()
            cache.then(savePendingMessages).then(cache)
        }
    }

    protected open fun doInTransaction(
        pjp: ProceedingJoinPoint,
        sendMqMessage: SendMqMessage,
        targetFlux: Flux<Any>
    ): Publisher<Any> {
        val savePendingMessages = parseMessages(
            targetFlux, (pjp.signature as MethodSignature).method
        ).flatMap { savePendingMessages(it, sendMqMessage) }
        val cache = targetFlux.cache()
        return cache.then(savePendingMessages).thenMany(cache)
    }

    protected open fun doInTransaction(
        pjp: ProceedingJoinPoint,
        sendMqMessage: SendMqMessage,
        targetMono: Mono<Any>
    ): Publisher<Any> {
        val savePendingMessages = parseMessages(
            targetMono, (pjp.signature as MethodSignature).method
        ).flatMap { savePendingMessages(it, sendMqMessage) }
        val cache = targetMono.cache()
        return cache.then(savePendingMessages).then(cache)
    }

    protected open fun parseMessages(
        returnVal: Any?,
        method: Method?
    ): Mono<MutableList<PendingMessage>> {
        if (returnVal is Mono<*>) {
            val clz = ResolvableType.forMethodReturnType(method)
                .getGeneric(0).rawClass
            if (PendingMessage::class.java == clz) {
                return returnVal.cast(PendingMessage::class.java)
                    .flatMap {
                        ReactivePendingMessageContextHolder.add(it)
                    }.then(ReactivePendingMessageContextHolder.get())
            } else if (Collection::class.java == clz) {
                return returnVal
                    .flatMap {
                        ReactivePendingMessageContextHolder.addAll(it.safeAs())
                    }.then(ReactivePendingMessageContextHolder.get())
            }
        } else if (returnVal is Flux<*>) {
            val clz = ResolvableType.forMethodReturnType(method)
                .getGeneric(0).rawClass
            if (PendingMessage::class.java == clz) {
                return returnVal.collectList()
                    .flatMap {
                        ReactivePendingMessageContextHolder.addAll(it.safeAs())
                    }.then(ReactivePendingMessageContextHolder.get())
            }
        }
        return ReactivePendingMessageContextHolder.get()
    }

    protected open fun savePendingMessages(
        pendingMessages: List<PendingMessage>,
        sendMqMessage: SendMqMessage
    ): Mono<Void> {
        return Mono.justOrEmpty(pendingMessages)
            .filter { CollectionUtils.isEmpty(it).not() }
            .flatMap {
                pendingMessages.forEach { pendingMessage: PendingMessage ->
                    pendingMessage.persistenceName = sendMqMessage.persistenceName
                    pendingMessage.transactionManager = sendMqMessage.transactionManager
                    pendingMessage.createTime = Date()
                }
                getReactiveProviderPersistence(sendMqMessage).save(pendingMessages)
            }
    }

    protected open fun getTransactionalOperator(sendMqMessage: SendMqMessage): TransactionalOperator {
        return SpringBeanUtils.getBean(
            sendMqMessage.transactionManager,
            TransactionalOperator::class.java
        )
    }

    protected open fun getReactiveProviderPersistence(sendMqMessage: SendMqMessage): ReactiveProviderPersistence {
        return SpringBeanUtils.getBean(
            sendMqMessage.persistenceName,
            ReactiveProviderPersistence::class.java
        )
    }

    override fun getOrder(): Int {
        return 0
    }

}
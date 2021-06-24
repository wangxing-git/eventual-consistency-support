package org.xyattic.eventual.consistency.support.core.persistence.reactive.impl

import org.springframework.jdbc.core.JdbcTemplate
import org.xyattic.eventual.consistency.support.core.consumer.ConsumedMessage
import org.xyattic.eventual.consistency.support.core.persistence.impl.JdbcTemplatePersistence
import org.xyattic.eventual.consistency.support.core.persistence.reactive.ReactivePersistence
import org.xyattic.eventual.consistency.support.core.provider.PendingMessage
import org.xyattic.eventual.consistency.support.core.provider.enums.PendingMessageStatus
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ForkJoinPool
import java.util.function.Supplier

/**
 * @author wangxing
 * @create 2020/11/13
 */
open class ReactiveJdbcTemplatePersistence : ReactivePersistence {

    val persistence: JdbcTemplatePersistence

    constructor(jdbcTemplate: JdbcTemplate) {
        persistence = JdbcTemplatePersistence(jdbcTemplate)
    }

    private val forkJoinPool = ForkJoinPool(20)

    override fun save(consumedMessage: ConsumedMessage): Mono<Void> {
        return Mono.fromFuture {
            CompletableFuture.runAsync(Runnable {
                persistence.save(consumedMessage)
            }, forkJoinPool)
        }
    }

    override fun save(pendingMessages: List<PendingMessage>): Mono<Void> {
        return Mono.fromFuture {
            CompletableFuture.runAsync(Runnable {
                persistence.save(pendingMessages)
            }, forkJoinPool)
        }
    }

    override fun changePendingMessageStatus(
        id: String,
        status: PendingMessageStatus,
        sendTime: Date
    ): Mono<Void> {
        return Mono.fromFuture {
            CompletableFuture.runAsync(Runnable {
                persistence.changePendingMessageStatus(id, status, sendTime)
            }, forkJoinPool)
        }
    }

    override fun sendSuccess(id: String, messageId: String): Mono<Void> {
        return Mono.fromFuture {
            CompletableFuture.runAsync(Runnable {
                persistence.sendSuccess(id, messageId)
            }, forkJoinPool)
        }
    }

    override fun sendFailed(id: String): Mono<Void> {
        return Mono.fromFuture {
            CompletableFuture.runAsync(Runnable {
                persistence.sendFailed(id)
            }, forkJoinPool)
        }
    }

    override fun getPendingMessages(timeBefore: Date): Flux<PendingMessage> {
        return Mono.fromFuture {
            CompletableFuture.supplyAsync(Supplier {
                persistence.getPendingMessages(timeBefore)
            }, forkJoinPool)
        }.flatMapIterable { it }
    }

}

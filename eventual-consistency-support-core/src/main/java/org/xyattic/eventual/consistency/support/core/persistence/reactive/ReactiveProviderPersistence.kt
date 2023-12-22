package org.xyattic.eventual.consistency.support.core.persistence.reactive

import org.xyattic.eventual.consistency.support.core.provider.PendingMessage
import org.xyattic.eventual.consistency.support.core.provider.enums.PendingMessageStatus
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.*

/**
 * @author wangxing
 * @create 2020/4/14
 */
interface ReactiveProviderPersistence {

    fun save(pendingMessage: PendingMessage): Mono<Void>

    fun changePendingMessageStatus(
        id: String,
        status: PendingMessageStatus,
        sendTime: Date
    ): Mono<Void>

    fun sendSuccess(id: String, messageId: String): Mono<Void>

    fun sendFailed(id: String): Mono<Void>

    fun getPendingMessages(timeBefore: Date): Flux<PendingMessage>

}
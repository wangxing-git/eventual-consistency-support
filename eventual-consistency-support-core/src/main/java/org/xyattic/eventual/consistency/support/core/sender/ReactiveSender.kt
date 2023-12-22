package org.xyattic.eventual.consistency.support.core.sender

import org.xyattic.eventual.consistency.support.core.provider.PendingMessage
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * @author wangxing
 * @create 2020/11/16
 */
interface ReactiveSender {

    fun send(pendingMessage: PendingMessage): Mono<Void>

    fun send(pendingMessages: List<PendingMessage>): Mono<Void> {
        return Flux.fromIterable(pendingMessages)
                .flatMap { send(it) }
                .then()
    }

}

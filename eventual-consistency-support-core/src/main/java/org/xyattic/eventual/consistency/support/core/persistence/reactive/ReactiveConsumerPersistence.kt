package org.xyattic.eventual.consistency.support.core.persistence.reactive

import org.xyattic.eventual.consistency.support.core.consumer.ConsumedMessage
import reactor.core.publisher.Mono

/**
 * @author wangxing
 * @create 2020/4/14
 */
interface ReactiveConsumerPersistence {
    fun save(consumedMessage: ConsumedMessage): Mono<Void>
}
package org.xyattic.eventual.consistency.support.core.provider

import reactor.core.publisher.Mono
import reactor.util.context.Context
import java.util.function.Function

/**
 * @author wangxing
 * @create 2020/4/1
 */
class ReactivePendingMessageContextHolder {
    companion object {

        @JvmStatic
        val KEY = ReactivePendingMessageContextHolder::class.java

        @JvmStatic
        fun get(): Mono<MutableList<PendingMessage>> {
            return Mono.subscriberContext()
//                    .subscriberContext { it.put(KEY, mutableListOf<MutableList<PendingMessage>>()) }
                    .map { it.get<MutableList<PendingMessage>>(KEY) }
        }

        @JvmStatic
        fun set(): Function<Context, Context> {
            return Function { it.put(KEY, mutableListOf<PendingMessage>()) }
        }

        @JvmStatic
        fun clear(): Function<Context, Context> {
            return Function { it.delete(KEY) }
        }

        @JvmStatic
        fun addAll(pendingMessages: Collection<PendingMessage>): Mono<Void> {
            return get()
                    .doOnNext { it.addAll(pendingMessages) }
                    .then()
        }

        @JvmStatic
        fun add(pendingMessage: PendingMessage): Mono<Void> {
            return get()
                    .doOnNext { it.add(pendingMessage) }
                    .then()
        }

    }

}

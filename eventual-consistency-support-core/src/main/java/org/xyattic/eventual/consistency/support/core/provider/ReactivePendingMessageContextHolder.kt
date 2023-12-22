package org.xyattic.eventual.consistency.support.core.provider

import reactor.core.publisher.Mono
import reactor.util.context.Context
import java.util.concurrent.CopyOnWriteArrayList
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
            return Function { it.put(KEY, CopyOnWriteArrayList<PendingMessage>()) }
        }

        @JvmStatic
        fun clear(): Mono<Void> {
            return Mono.subscriberContext()
                .doOnNext {
                    if (it.hasKey(KEY)) {
                        it.get<MutableList<PendingMessage>>(KEY).clear();
                    }
                }
                .then()
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

        fun remove(pendingMessage: PendingMessage): Mono<Void>{
            return get()
                    .doOnNext {
                        it.remove(pendingMessage)
                    }
                    .then()
        }

    }

}

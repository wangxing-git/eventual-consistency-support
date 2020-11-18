package org.xyattic.eventual.consistency.support.core.provider

import com.google.common.collect.Lists

/**
 * @author wangxing
 * @create 2020/4/1
 */
class PendingMessageContextHolder {
    companion object{
        private val contextHolder: ThreadLocal<MutableList<PendingMessage>> = ThreadLocal.withInitial { mutableListOf<PendingMessage>() }

        @JvmStatic
        fun set(pendingMessages: List<PendingMessage>) {
            contextHolder.set(Lists.newArrayList(pendingMessages))
        }

        @JvmStatic
        fun clear() {
            contextHolder.remove()
        }

        @JvmStatic
        fun get(): List<PendingMessage> {
            return contextHolder.get()
        }

        @JvmStatic
        fun addAll(pendingMessages: List<PendingMessage>) {
            contextHolder.get().addAll(pendingMessages)
        }

        @JvmStatic
        fun add(pendingMessage: PendingMessage) {
            contextHolder.get().add(pendingMessage)
        }

    }

}
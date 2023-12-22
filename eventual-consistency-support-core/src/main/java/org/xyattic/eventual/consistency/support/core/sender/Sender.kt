package org.xyattic.eventual.consistency.support.core.sender

import org.xyattic.eventual.consistency.support.core.provider.PendingMessage

/**
 * @author wangxing
 * @create 2020/11/16
 */
interface Sender {

    fun send(pendingMessage: PendingMessage)

    fun send(pendingMessages: List<PendingMessage>) {
        pendingMessages.forEach { send(it) }
    }

}
package org.xyattic.eventual.consistency.support.core.persistence

import org.xyattic.eventual.consistency.support.core.provider.PendingMessage
import org.xyattic.eventual.consistency.support.core.provider.enums.PendingMessageStatus
import java.util.*

/**
 * @author wangxing
 * @create 2020/4/14
 */
interface ProviderPersistence {

    fun save(pendingMessage: PendingMessage)

    fun changePendingMessageStatus(id: String, status: PendingMessageStatus, sendTime: Date)

    fun sendSuccess(id: String, messageId: String)

    fun sendFailed(id: String)

    fun getPendingMessages(timeBefore: Date): List<PendingMessage>

}
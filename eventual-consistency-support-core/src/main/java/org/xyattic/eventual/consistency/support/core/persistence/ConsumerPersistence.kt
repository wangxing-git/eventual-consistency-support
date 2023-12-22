package org.xyattic.eventual.consistency.support.core.persistence

import org.xyattic.eventual.consistency.support.core.consumer.ConsumedMessage

/**
 * @author wangxing
 * @create 2020/4/14
 */
interface ConsumerPersistence {
    fun save(consumedMessage: ConsumedMessage)
}
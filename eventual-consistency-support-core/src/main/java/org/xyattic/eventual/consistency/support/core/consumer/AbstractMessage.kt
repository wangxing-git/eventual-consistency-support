package org.xyattic.eventual.consistency.support.core.consumer

import java.io.Serializable

/**
 * @author wangxing
 * @create 2021/6/24
 */
abstract class AbstractMessage : Serializable {

    /**
     * 消息中间件消息id
     */
    var msgId: String? = null

    /**
     * 是否可重新消费
     */
    val reconsume: Boolean
        get() {
            return reconsumeTimes < maxReconsumeTimes
        }

    /**
     * 业务消息id
     */
    abstract fun getId(): String

    /**
     * 重新消费次数
     */
    var reconsumeTimes: Int = 0

    /**
     * 最大消费次数
     */
    var maxReconsumeTimes: Int = 10

    companion object {
        private const val serialVersionUID: Long = -5821063407906602150L
    }

}

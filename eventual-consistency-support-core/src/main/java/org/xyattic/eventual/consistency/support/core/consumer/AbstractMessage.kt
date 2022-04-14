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
    var reconsume: Boolean = true

    /**
     * 业务消息id
     */
    abstract fun getId(): String

}

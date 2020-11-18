package org.xyattic.eventual.consistency.support.core.properties

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * @author wangxing
 * @create 2020/11/17
 */
@ConfigurationProperties("eventual-consistency")
class EventualConsistencyProperties {

    var enabled = true
    var databaseType: DatabaseType? = null
    var senderType: SenderType? = null

    enum class DatabaseType {
        mongodb, jdbc, In_Memory
    }

    enum class SenderType {
        rocketmq, rabbitmq
    }

}
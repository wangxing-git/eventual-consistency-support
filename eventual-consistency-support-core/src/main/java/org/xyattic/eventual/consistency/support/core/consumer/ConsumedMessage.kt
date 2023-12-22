package org.xyattic.eventual.consistency.support.core.consumer

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.xyattic.eventual.consistency.support.core.annotation.Data
import java.util.*

/**
 * @author wangxing
 * @create 2020/3/23
 */
@Data
@Document("consumedMessages")
data class ConsumedMessage(
        @Id
        var id: Any,
        var message: Any,
        var createTime: Date = Date(),
        var consumeTime: Date? = null,
        var success: Boolean = false,
        var exception: String? = null
)
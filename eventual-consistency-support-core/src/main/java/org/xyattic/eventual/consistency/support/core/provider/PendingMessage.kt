package org.xyattic.eventual.consistency.support.core.provider

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.xyattic.eventual.consistency.support.core.annotation.Data
import org.xyattic.eventual.consistency.support.core.provider.enums.PendingMessageStatus
import java.io.Serializable
import java.util.*

/**
 * @author wangxing
 * @create 2020/4/1
 */
@Data
@Document("pendingMessages")
data class PendingMessage(
        @Id
        var messageId: String,
        var body: Any,
        var destination: String,
        var status: PendingMessageStatus = PendingMessageStatus.PENDING,
        var persistenceName: String? = null,
        var transactionManager: String? = null,
        var createTime: Date = Date(),
        var sendTime: Date? = null,
        var headers: MutableMap<String, Any> = linkedMapOf()
) : Serializable {

    fun setHeader(key: String, value: Any) {
        headers[key] = value
    }

//    constructor() : this("","","")

//    constructor(messageId: String, body: Any, destination: String) : this(messageId,body,destination,PendingMessageStatus.PENDING)

    companion object {

        @JvmStatic
        fun builder(messageId: String, body: Any, destination: String): Builder {
            return Builder().messageId(messageId)
                    .body(body)
                    .destination(destination)
        }

        @JvmStatic
        fun builder(messageId: String): Builder {
            return Builder().messageId(messageId)
        }

        @JvmStatic
        fun builder(): Builder {
            return Builder()
        }

    }

    class Builder {

        private lateinit var messageId: String

        private lateinit var body: Any

        private lateinit var destination: String

        private var status: PendingMessageStatus = PendingMessageStatus.PENDING

        private var persistenceName: String? = null

        private var transactionManager: String? = null

        private var createTime: Date = Date()

        private var sendTime: Date? = null

        private var headers: MutableMap<String, Any> = linkedMapOf()

        fun setHeader(headerName: String, headerValue: Any): Builder {
            headers[headerName] = headerValue
            return this
        }

        fun messageId(messageId: String): Builder {
            this.messageId = messageId
            return this
        }

        fun body(body: Any): Builder {
            this.body = body
            return this
        }

        fun destination(destination: String): Builder {
            this.destination = destination
            return this
        }

        fun status(status: PendingMessageStatus): Builder {
            this.status = status
            return this
        }

        fun persistenceName(persistenceName: String): Builder {
            this.persistenceName = persistenceName
            return this
        }

        fun transactionManager(transactionManager: String): Builder {
            this.transactionManager = transactionManager
            return this
        }

        fun createTime(createTime: Date): Builder {
            this.createTime = createTime
            return this
        }

        fun build(): PendingMessage {
            return PendingMessage(messageId, body, destination, status, persistenceName,
                    transactionManager, createTime, sendTime, headers)
        }

    }

}

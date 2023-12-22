package org.xyattic.eventual.consistency.support.core.persistence.impl

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.BeanUtils
import org.springframework.jdbc.core.BeanPropertyRowMapper
import org.springframework.jdbc.core.JdbcTemplate
import org.xyattic.eventual.consistency.support.core.annotation.Data
import org.xyattic.eventual.consistency.support.core.consumer.ConsumedMessage
import org.xyattic.eventual.consistency.support.core.persistence.Persistence
import org.xyattic.eventual.consistency.support.core.provider.PendingMessage
import org.xyattic.eventual.consistency.support.core.provider.PendingMessageHeaders
import org.xyattic.eventual.consistency.support.core.provider.enums.PendingMessageStatus
import java.io.Serializable
import java.util.*
import java.util.stream.Collectors

/**
 * @author wangxing
 * @create 2020/11/13
 */
open class JdbcTemplatePersistence : Persistence {

    private val jdbcTemplate: JdbcTemplate
    private val objectMapper: ObjectMapper

    constructor(jdbcTemplate: JdbcTemplate,
                objectMapper: ObjectMapper) {
        this.jdbcTemplate = jdbcTemplate
        this.objectMapper = objectMapper
    }

    override fun save(consumedMessage: ConsumedMessage) {
        jdbcTemplate.update(
                "INSERT INTO consumed_message (id,message,success,exception," +
                        "consume_time,create_time) VALUES (?,?,?,?,?,?)",
                consumedMessage.id.toString(),
                objectMapper.writeValueAsString(consumedMessage.message),
                if (consumedMessage.success) 1 else 0, consumedMessage.exception,
                consumedMessage.consumeTime, consumedMessage.createTime
        )
    }

    override fun save(pendingMessage: PendingMessage) {
        jdbcTemplate.update(
                "INSERT INTO pending_message (message_id,body,destination," +
                        "headers,`status`,persistence_name,transaction_manager,create_time) " +
                        "VALUES (?,?,?,?,?,?,?,?)",
                pendingMessage.messageId,
                objectMapper.writeValueAsString(pendingMessage.body),
                pendingMessage.destination,
                objectMapper.writeValueAsString(pendingMessage.headers),
                pendingMessage.status.toString(), pendingMessage.persistenceName,
                pendingMessage.transactionManager, pendingMessage.createTime
        )
    }

    override fun changePendingMessageStatus(
            id: String,
            status: PendingMessageStatus,
            sendTime: Date
    ) {
        jdbcTemplate.update(
                "UPDATE pending_message SET `status`=?,`send_time`=? WHERE message_id=?",
                status.toString(), sendTime, id
        )
    }

    override fun sendSuccess(id: String, messageId: String) {
        jdbcTemplate.query(
                "SELECT*FROM pending_message WHERE message_id=?",
                BeanPropertyRowMapper(PendingMessageEntity::class.java), id
        ).firstOrNull()?.apply {
            val mutableMap =
                    objectMapper.readValue(headers, object : TypeReference<MutableMap<String, Any>>() {})
            mutableMap[PendingMessageHeaders.msgIdHeader] = messageId
            jdbcTemplate.update(
                    "UPDATE pending_message SET `status`=?,`send_time`=?,`headers`=? WHERE message_id=?",
                    PendingMessageStatus.HAS_BEEN_SENT.toString(), Date(),
                    objectMapper.writeValueAsString(mutableMap), id
            )
        }
    }

    override fun sendFailed(id: String) {
        jdbcTemplate.update(
                "UPDATE pending_message SET `status`=? WHERE message_id=?",
                PendingMessageStatus.FAILED_TO_SEND.toString(), id
        )
    }

    override fun getPendingMessages(timeBefore: Date): List<PendingMessage> {
        return jdbcTemplate.query(
                "SELECT*FROM pending_message WHERE `status`=? AND create_time<?",
                BeanPropertyRowMapper(PendingMessageEntity::class.java),
                PendingMessageStatus.PENDING.toString(), timeBefore
        ).stream()
                .map { pendingMessageEntity: PendingMessageEntity ->
                    val pendingMessage =
                            pendingMessageEntity.run { PendingMessage(messageId, body, destination) }
                    BeanUtils.copyProperties(
                            pendingMessageEntity, pendingMessage, "body",
                            "headers"
                    )
                    pendingMessage.apply {
                        body = objectMapper.readValue<Any>(pendingMessageEntity.body, Any::class.java)
                        headers = objectMapper.readValue(pendingMessageEntity.headers, object : TypeReference<MutableMap<String, Any>>() {})
                    }
                }.collect(Collectors.toList())
    }

    @Data
    internal data class PendingMessageEntity(var messageId: String) : Serializable {

        lateinit var body: String
        lateinit var destination: String
        lateinit var status: PendingMessageStatus
        var persistenceName: String? = null
        var transactionManager: String? = null
        lateinit var createTime: Date
        lateinit var headers: String
        var sendTime: Date? = null

    }
}

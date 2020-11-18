package org.xyattic.eventual.consistency.support.core.persistence.impl

import com.alibaba.fastjson.JSONObject
import com.alibaba.fastjson.parser.Feature
import com.alibaba.fastjson.serializer.SerializerFeature
import org.springframework.beans.BeanUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.BeanPropertyRowMapper
import org.springframework.jdbc.core.JdbcTemplate
import org.xyattic.eventual.consistency.support.core.annotation.Data
import org.xyattic.eventual.consistency.support.core.consumer.ConsumedMessage
import org.xyattic.eventual.consistency.support.core.persistence.Persistence
import org.xyattic.eventual.consistency.support.core.provider.PendingMessage
import org.xyattic.eventual.consistency.support.core.provider.enums.PendingMessageStatus
import java.io.Serializable
import java.util.*
import java.util.function.Consumer
import java.util.stream.Collectors

/**
 * @author wangxing
 * @create 2020/11/13
 */
open class JdbcTemplatePersistence : Persistence {

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    override fun save(consumedMessage: ConsumedMessage) {
        jdbcTemplate.update("INSERT INTO consumed_message (id,message,success,exception," +
                "consume_time,create_time) VALUES (?,?,?,?,?,?)",
                consumedMessage.id.toString(),
                JSONObject.toJSONString(consumedMessage.message,
                        SerializerFeature.WriteClassName),
                if (consumedMessage.success) 1 else 0, consumedMessage.exception,
                consumedMessage.consumeTime, consumedMessage.createTime)
    }

    override fun save(pendingMessages: List<PendingMessage>) {
        pendingMessages.forEach(Consumer { pendingMessage: PendingMessage ->
            jdbcTemplate.update("INSERT INTO pending_message (message_id,body,destination," +
                    "headers,`status`,persistence_name,transaction_manager,create_time) " +
                    "VALUES (?,?,?,?,?,?,?,?)",
                    pendingMessage.messageId,
                    JSONObject.toJSONString(pendingMessage.body,
                            SerializerFeature.WriteClassName),
                    pendingMessage.destination,
                    JSONObject.toJSONString(pendingMessage.headers,
                            SerializerFeature.WriteClassName),
                    pendingMessage.status.toString(), pendingMessage.persistenceName,
                    pendingMessage.transactionManager, pendingMessage.createTime)
        })
    }

    override fun changePendingMessageStatus(id: String, status: PendingMessageStatus, sendTime: Date) {
        jdbcTemplate.update("UPDATE pending_message SET `status`=?,`send_time`=? WHERE message_id=?",
                status.toString(), sendTime, id)
    }

    override fun getPendingMessages(timeBefore: Date): List<PendingMessage> {
        return jdbcTemplate.query("SELECT*FROM pending_message WHERE `status`=? AND create_time<?",
                BeanPropertyRowMapper(PendingMessageEntity::class.java),
                PendingMessageStatus.PENDING.toString(), timeBefore).stream()
                .map { pendingMessageEntity: PendingMessageEntity ->
                    val pendingMessage = pendingMessageEntity.run { PendingMessage(messageId, body, destination) }
                    BeanUtils.copyProperties(pendingMessageEntity, pendingMessage, "body",
                            "headers")
                    pendingMessage.apply {
                        body = JSONObject.parse(pendingMessageEntity.body, Feature.SupportAutoType)
                        headers = JSONObject.parse(pendingMessageEntity.headers, Feature.SupportAutoType) as MutableMap<String, Any>
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

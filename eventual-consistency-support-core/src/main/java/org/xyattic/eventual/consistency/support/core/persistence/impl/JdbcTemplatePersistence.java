package org.xyattic.eventual.consistency.support.core.persistence.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.serializer.SerializerFeature;
import lombok.Data;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.xyattic.eventual.consistency.support.core.consumer.ConsumedMessage;
import org.xyattic.eventual.consistency.support.core.persistence.Persistence;
import org.xyattic.eventual.consistency.support.core.provider.PendingMessage;
import org.xyattic.eventual.consistency.support.core.provider.enums.PendingMessageStatus;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author wangxing
 * @create 2020/11/13
 */
public class JdbcTemplatePersistence implements Persistence {

    @Autowired
    private JdbcTemplate jdbcTemplate;
//    @Autowired
//    private TransactionTemplate transactionTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(ConsumedMessage consumedMessage) {
        jdbcTemplate.update("INSERT INTO consumed_message (id,message,success,exception," +
                        "create_time) VALUES (?,?,?,?,?)",
                consumedMessage.getId().toString(),
                JSONObject.toJSONString(consumedMessage.getMessage(),
                        SerializerFeature.WriteClassName),
                consumedMessage.getSuccess() ? 1 : 0, consumedMessage.getException(),
                consumedMessage.getCreateTime());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(List<PendingMessage> pendingMessages) {
        pendingMessages.forEach(pendingMessage -> {
            jdbcTemplate.update("INSERT INTO pending_message (message_id,body,destination," +
                            "headers,`status`,persistence_name,transaction_manager,create_time) " +
                            "VALUES (?,?,?,?,?,?,?,?)",
                    pendingMessage.getMessageId(),
                    JSONObject.toJSONString(pendingMessage.getBody(),
                            SerializerFeature.WriteClassName),
                    pendingMessage.getDestination(),
                    JSONObject.toJSONString(pendingMessage.getHeaders(),
                            SerializerFeature.WriteClassName),
                    pendingMessage.getStatus().toString(), pendingMessage.getPersistenceName(),
                    pendingMessage.getTransactionManager(), pendingMessage.getCreateTime());
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePendingMessageStatus(String id, PendingMessageStatus status) {
        jdbcTemplate.update("UPDATE pending_message SET `status`=? WHERE message_id=?",
                status.toString(), id);
    }

    @Override
    public List<PendingMessage> getPendingMessages(Date timeBefore) {
        return jdbcTemplate.query("SELECT*FROM pending_message WHERE `status`=? AND create_time<?",
                new BeanPropertyRowMapper<>(PendingMessageEntity.class),
                PendingMessageStatus.PENDING.toString(), timeBefore).stream()
                .map(pendingMessageEntity -> {
                    final PendingMessage pendingMessage = new PendingMessage();
                    BeanUtils.copyProperties(pendingMessageEntity, pendingMessage, "body",
                            "headers");
                    pendingMessage.setBody(JSONObject.parse(pendingMessageEntity.getBody(),
                            Feature.SupportAutoType));
                    pendingMessage.setHeaders(((Map<String, Object>) JSONObject.parse(pendingMessageEntity.getHeaders(), Feature.SupportAutoType)));
                    return pendingMessage;
                }).collect(Collectors.toList());
    }

    @Data
    static class PendingMessageEntity implements Serializable {

        private String messageId;

        private String body;

        private String destination;

        private String headers;

        private PendingMessageStatus status = PendingMessageStatus.PENDING;

        private String persistenceName;

        private String transactionManager;

        private Date createTime;

    }

}
package org.xyattic.eventual.consistency.support.core.sender.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.transaction.annotation.Transactional;
import org.xyattic.eventual.consistency.support.core.persistence.ProviderPersistence;
import org.xyattic.eventual.consistency.support.core.provider.PendingMessage;
import org.xyattic.eventual.consistency.support.core.provider.enums.PendingMessageStatus;
import org.xyattic.eventual.consistency.support.core.sender.Sender;

/**
 * @author wangxing
 * @create 2020/6/29
 */
@Slf4j
public class RocketSender implements Sender {

    @Autowired
    private RocketMQTemplate rocketMQTemplate;
    @Autowired
    private ProviderPersistence providerPersistence;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void send(PendingMessage pendingMessage) {
        SendResult sendResult = rocketMQTemplate.syncSend(pendingMessage.getDestination(),
                new GenericMessage<>(pendingMessage.getBody(), pendingMessage.getHeaders()));
        if (SendStatus.SEND_OK.equals(sendResult.getSendStatus())) {
            log.info("发送成功:{}", sendResult);
            providerPersistence.changePendingMessageStatus(pendingMessage.getMessageId(),
                    PendingMessageStatus.HAS_BEEN_SENT);
        } else {
            log.warn("发送失败:" + sendResult);
        }
    }

}
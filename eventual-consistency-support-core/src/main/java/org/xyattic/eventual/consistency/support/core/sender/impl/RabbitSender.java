package org.xyattic.eventual.consistency.support.core.sender.impl;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;
import org.xyattic.eventual.consistency.support.core.constants.RabbitConstants;
import org.xyattic.eventual.consistency.support.core.exception.MqException;
import org.xyattic.eventual.consistency.support.core.persistence.ProviderPersistence;
import org.xyattic.eventual.consistency.support.core.provider.PendingMessage;
import org.xyattic.eventual.consistency.support.core.provider.aop.SendMqMessage;
import org.xyattic.eventual.consistency.support.core.provider.enums.PendingMessageStatus;
import org.xyattic.eventual.consistency.support.core.sender.Sender;
import org.xyattic.eventual.consistency.support.core.utils.RabbitUtils;
import org.xyattic.eventual.consistency.support.core.utils.SpringBeanUtils;

/**
 * @author wangxing
 * @create 2020/4/1
 */
@Slf4j
public class RabbitSender implements Sender, InitializingBean {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public void send(PendingMessage pendingMessage) {
        try {
            CorrelationData correlationData = new CorrelationData();
            correlationData.setId(pendingMessage.getMessageId());
            byte[] bytes = JSON.toJSONString(pendingMessage.getBody()).getBytes();
            MessageProperties properties = new MessageProperties();
            properties.setHeader("pendingMessage", pendingMessage);
            Message message = new Message(bytes, properties);
            correlationData.setReturnedMessage(message);
            MessagePostProcessor messagePostProcessor = new MessagePostProcessor() {
                @Override
                public Message postProcessMessage(Message message) throws AmqpException {
                    message.getMessageProperties().setMessageId(pendingMessage.getMessageId());
                    message.getMessageProperties().setAppId(pendingMessage.getHeaders().get(RabbitConstants.APP_ID_HEADER).toString());
                    return message;
                }
            };
            rabbitTemplate.convertAndSend(RabbitUtils.getExchange(pendingMessage.getHeaders().get(RabbitConstants.EXCHANGE_HEADER).toString()),
                    RabbitUtils.getRoutingKey(pendingMessage.getDestination()),
                    pendingMessage.getBody(), messagePostProcessor, correlationData);
            log.info("成功发送消息:{}", pendingMessage);
        } catch (Exception e) {
            log.warn("消息发送失败,pendingMessage:" + pendingMessage, e);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        rabbitTemplate.setConfirmCallback((correlationData1, ack, cause) -> {
            if (ack) {
                changeMessageStatusSuccess(correlationData1);
            } else {
                log.warn("确认消息失败:{},correlationData:{}", cause, correlationData1);
            }
        });
        rabbitTemplate.setReturnCallback((message1, replyCode, replyText, exchange, routingKey) -> {
            log.error("消息发送失败:message:{},replyCode:{},replyText:{},exchange:{},routingKey:{}",
                    message1, replyCode, replyText, exchange, routingKey);
        });
    }

    protected void changeMessageStatusSuccess(CorrelationData correlationData) {
        PendingMessage pendingMessage =
                correlationData.getReturnedMessage().getMessageProperties().getHeader(
                        "pendingMessage");
        if (pendingMessage != null) {
            getTransactionTemplate(pendingMessage.getTransactionManager()).executeWithoutResult(transactionStatus -> {
                getProviderPersistence(pendingMessage.getPersistenceName()).changePendingMessageStatus(correlationData.getId(), PendingMessageStatus.HAS_BEEN_SENT);
            });
        }
    }

    protected TransactionTemplate getTransactionTemplate(SendMqMessage sendMqMessage) {
        return getBean(sendMqMessage.transactionManager(), TransactionTemplate.class);
    }

    protected ProviderPersistence getProviderPersistence(SendMqMessage sendMqMessage) {
        return getBean(sendMqMessage.persistenceName(), ProviderPersistence.class);
    }

    protected TransactionTemplate getTransactionTemplate(String name) {
        return getBean(name, TransactionTemplate.class);
    }

    protected ProviderPersistence getProviderPersistence(String name) {
        return getBean(name, ProviderPersistence.class);
    }

    protected <T> T getBean(String beanName, Class<T> beanClass) {
        T bean;
        if (StringUtils.isNotBlank(beanName)) {
            bean = SpringBeanUtils.getBean(beanName);
        } else {
            bean = SpringBeanUtils.getBeanFactory().getBeanProvider(beanClass).getIfUnique();
            if (bean == null) {
                final String[] beanNamesForType =
                        SpringBeanUtils.getBeanFactory().getBeanNamesForType(beanClass);
                if (beanNamesForType.length == 0) {
                    throw new MqException(beanClass.getSimpleName() + " bean not found in " +
                            "spring");
                } else if (beanNamesForType.length == 1) {
                    bean = SpringBeanUtils.getBean(beanNamesForType[0]);
                } else {
                    throw new MqException(beanClass.getSimpleName() + " bean found " + beanNamesForType.length + ", please specify a name");
                }
            }
        }
        return bean;
    }

}
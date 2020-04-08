package org.xyattic.eventual.consistency.support.core.rabbit.provider;

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
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;
import org.xyattic.boot.calkin.core.exception.BaseException;
import org.xyattic.boot.calkin.core.utils.SpringBeanUtils;
import org.xyattic.eventual.consistency.support.core.rabbit.RabbitUtils;
import org.xyattic.eventual.consistency.support.core.rabbit.provider.aop.SendMqMessage;
import org.xyattic.eventual.consistency.support.core.rabbit.provider.enums.PendingMessageStatus;

import java.util.List;

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
                    message.getMessageProperties().setAppId(pendingMessage.getAppId());
                    return message;
                }
            };
            rabbitTemplate.convertAndSend(RabbitUtils.getExchange(pendingMessage.getExchange()),
                    RabbitUtils.getRoutingKey(pendingMessage.getRoutingKey()),
                    pendingMessage.getBody(), messagePostProcessor, correlationData);
        } catch (Exception e) {
            log.warn("消息发送失败,pendingMessage:" + pendingMessage, e);
        }
    }

    @Override
    public void send(List<PendingMessage> pendingMessages) {
        if (!CollectionUtils.isEmpty(pendingMessages)) {
            pendingMessages.forEach(this::send);
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
                MongoTemplate mongoTemplate = getMongoTemplate(pendingMessage.getMongoTemplate());
                Query query = Query.query(Criteria.where("_id").is(correlationData.getId()));
                Update update = Update.update("status", PendingMessageStatus.HAS_BEEN_SENT);
                mongoTemplate.update(PendingMessage.class).matching(query).apply(update).all();
            });
        }
    }

    protected TransactionTemplate getTransactionTemplate(SendMqMessage sendMqMessage) {
        return getBean(sendMqMessage.transactionManager(), TransactionTemplate.class);
    }

    protected MongoTemplate getMongoTemplate(SendMqMessage sendMqMessage) {
        return getBean(sendMqMessage.mongoTemplate(), MongoTemplate.class);
    }

    protected TransactionTemplate getTransactionTemplate(String name) {
        return getBean(name, TransactionTemplate.class);
    }

    protected MongoTemplate getMongoTemplate(String name) {
        return getBean(name, MongoTemplate.class);
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
                    throw new BaseException(beanClass.getSimpleName() + " bean not found in " +
                            "spring");
                } else if (beanNamesForType.length == 1) {
                    bean = SpringBeanUtils.getBean(beanNamesForType[0]);
                } else {
                    throw new BaseException(beanClass.getSimpleName() + " bean found " + beanNamesForType.length + ", please specify a name");
                }
            }
        }
        return bean;
    }

}
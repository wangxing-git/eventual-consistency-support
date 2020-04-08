package org.xyattic.eventual.consistency.support.core.rabbit.handler;

import com.alibaba.fastjson.JSONObject;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.HandlerMethod;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.ObjectUtils;
import org.xyattic.boot.calkin.core.exception.BaseException;
import org.xyattic.boot.calkin.core.utils.SpringBeanUtils;
import org.xyattic.eventual.consistency.support.core.rabbit.ConsumedMessage;
import org.xyattic.eventual.consistency.support.core.rabbit.aop.RabbitConsumer;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;

/**
 * @author wangxing
 * @create 2020/3/27
 */
@Slf4j
public class InvocableHandlerMethod extends org.springframework.messaging.handler.invocation.InvocableHandlerMethod {

    protected static final String CONSUMED_COUNT_HEADER_NAME = "consumedCount";

    protected Integer maxConsume = 10;

    protected TransactionTemplate getTransactionTemplate(RabbitConsumer rabbitConsumer) {
        return getBean(rabbitConsumer.transactionManager(), TransactionTemplate.class);
    }

    protected MongoTemplate getMongoTemplate(RabbitConsumer rabbitConsumer) {
        return getBean(rabbitConsumer.mongoTemplate(), MongoTemplate.class);
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

    public InvocableHandlerMethod(HandlerMethod handlerMethod) {
        super(handlerMethod);
    }

    public InvocableHandlerMethod(Object bean, Method method) {
        super(bean, method);
    }

    public InvocableHandlerMethod(Object bean, String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
        super(bean, methodName, parameterTypes);
    }

    @Override
    @SneakyThrows
    public Object invoke(Message<?> message, Object... providedArgs) throws Exception {
        RabbitConsumer rabbitConsumer = AnnotatedElementUtils.findMergedAnnotation(getMethod(),
                RabbitConsumer.class);
        if (rabbitConsumer != null) {
            boolean debugEnabled = log.isDebugEnabled();
            TransactionTemplate transactionTemplate = getTransactionTemplate(rabbitConsumer);

            org.springframework.amqp.core.Message amqpMessage =
                    Objects.requireNonNull(findProvidedArgument(org.springframework.amqp.core.Message.class, providedArgs));
            Channel channel = Objects.requireNonNull(findProvidedArgument(Channel.class,
                    providedArgs));

            //设置每次从队列里取出一条处理
            channel.basicQos(1);
            try {
                Object obj = transactionTemplate.execute(status -> invoke(message, rabbitConsumer
                        , amqpMessage, providedArgs));
                channel.basicAck(amqpMessage.getMessageProperties().getDeliveryTag(), false);
                return obj;
            } catch (DuplicateKeyException d) {
                log.info("重复的消息,已忽略,message:{}", amqpMessage);
                //忽略重复的消息
                channel.basicAck(amqpMessage.getMessageProperties().getDeliveryTag(), false);
            } catch (Exception e) {
                log.warn("消费失败,message:" + amqpMessage, e);
                if (e.getMessage().contains(" in multi-document transaction.")) {
                    log.info("缺失必要集合,将进行创建");
                    //创建集合
                    getMongoTemplate(rabbitConsumer).createCollection(ConsumedMessage.class);
                }
                Integer count = getConsumedCount(amqpMessage);
                if (count >= maxConsume) {
                    log.info("消费次数已达最大限制:{},将丢弃该消息message:{}", count, amqpMessage);
                    saveFailConsumedMessages(rabbitConsumer, amqpMessage, e);
                } else {
                    log.info("进行消息重发,message:{}", amqpMessage);
                    resend(amqpMessage, channel);
                }
                channel.basicAck(amqpMessage.getMessageProperties().getDeliveryTag(), false);
            }
            return null;
        } else {
            return super.invoke(message, providedArgs);
        }
    }

    protected Integer getConsumedCount(org.springframework.amqp.core.Message amqpMessage) {
        return Optional.ofNullable(
                (Integer) amqpMessage.getMessageProperties().getHeader(CONSUMED_COUNT_HEADER_NAME)).orElse(1);
    }

    protected void resend(org.springframework.amqp.core.Message amqpMessage, Channel channel) throws IOException {
        MessageProperties messageProperties = amqpMessage.getMessageProperties();
        Integer count = getConsumedCount(amqpMessage);
        messageProperties.setHeader(CONSUMED_COUNT_HEADER_NAME, count + 1);
        AMQP.BasicProperties basicProperties = new AMQP.BasicProperties(
                messageProperties.getContentType(),
                messageProperties.getContentEncoding(), messageProperties.getHeaders(),
                MessageDeliveryMode.toInt(messageProperties.getReceivedDeliveryMode()),
                messageProperties.getPriority(), messageProperties.getCorrelationId(),
                messageProperties.getReplyTo(), messageProperties.getExpiration(),
                messageProperties.getMessageId(), messageProperties.getTimestamp(),
                messageProperties.getType(), messageProperties.getUserId(),
                messageProperties.getAppId(), messageProperties.getClusterId());
        channel.basicPublish(amqpMessage.getMessageProperties().getReceivedExchange(),
                amqpMessage.getMessageProperties().getReceivedRoutingKey(),
                basicProperties, amqpMessage.getBody());
    }

    @SneakyThrows
    protected Object invoke(Message<?> message, RabbitConsumer rabbitConsumer,
                            org.springframework.amqp.core.Message amqpMessage,
                            Object[] providedArgs) {
        saveSuccessConsumedMessages(rabbitConsumer, amqpMessage);
        return super.invoke(message, providedArgs);
    }

    protected void saveSuccessConsumedMessages(RabbitConsumer rabbitConsumer,
                                               org.springframework.amqp.core.Message message) {
        saveConsumedMessages(rabbitConsumer, message, true, null);
    }

    protected void saveFailConsumedMessages(RabbitConsumer rabbitConsumer,
                                            org.springframework.amqp.core.Message message,
                                            Exception e) {
        saveConsumedMessages(rabbitConsumer, message, false, e);
    }

    protected void saveConsumedMessages(RabbitConsumer rabbitConsumer,
                                        org.springframework.amqp.core.Message message,
                                        boolean success, Exception e) {
        JSONObject jsonObject = (JSONObject) JSONObject.parse(message.getBody());
        JSONObject m = JSONObject.parseObject(JSONObject.toJSONString(message));
        m.put("body", jsonObject);
        ConsumedMessage consumedMessage = new ConsumedMessage();
        consumedMessage.setId(message.getMessageProperties().getMessageId());
        consumedMessage.setSuccess(success);
        consumedMessage.setMessage(m);
        if (e != null) {
            consumedMessage.setException(ExceptionUtils.getStackTrace(e));
        }
        getMongoTemplate(rabbitConsumer).insert(consumedMessage);
    }

    protected static <T> T findProvidedArgument(Class<T> parameter,
                                                @Nullable Object... providedArgs) {
        if (!ObjectUtils.isEmpty(providedArgs)) {
            for (Object providedArg : providedArgs) {
                if (parameter.isInstance(providedArg)) {
                    return parameter.cast(providedArg);
                }
            }
        }
        return null;
    }

}
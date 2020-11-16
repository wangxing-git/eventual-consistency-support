package org.xyattic.eventual.consistency.support.core.consumer.aop;

import com.google.common.collect.Maps;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.lang.Nullable;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.ObjectUtils;
import org.xyattic.eventual.consistency.support.core.aop.support.AnnotationMethodInterceptor;
import org.xyattic.eventual.consistency.support.core.consumer.ConsumedMessage;
import org.xyattic.eventual.consistency.support.core.exception.MqException;
import org.xyattic.eventual.consistency.support.core.persistence.ConsumerPersistence;
import org.xyattic.eventual.consistency.support.core.utils.SpelUtils;
import org.xyattic.eventual.consistency.support.core.utils.SpringBeanUtils;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * @author wangxing
 * @create 2020/8/24
 */
@Slf4j
public class ConsumeMqMessageInterceptor extends AnnotationMethodInterceptor<ConsumeMqMessage> {

    @Override
    protected Object doInvoke(ConsumeMqMessage consumeMqMessage, ProceedingJoinPoint pjp) {
        Serializable message = parseMessage(pjp, consumeMqMessage);

        Object id = parseMessageId(pjp, consumeMqMessage, message);

        try {
            return getTransactionTemplate(consumeMqMessage).execute(status -> doInTransaction(pjp,
                    consumeMqMessage, id, message));
        } catch (DuplicateKeyException duplicateKeyException) {
            log.info("Duplicate message, ignored, id: {}, message: {}", id, message);
            return null;
        }
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

    @SneakyThrows
    protected Object doInTransaction(ProceedingJoinPoint pjp, ConsumeMqMessage consumeMqMessage,
                                     Object id, Serializable message) {
        ConsumedMessage consumedMessage = new ConsumedMessage();
        consumedMessage.setId(id);
        consumedMessage.setMessage(message);

        saveSuccessConsumedMessages(consumeMqMessage, consumedMessage);

        return pjp.proceed();
    }

    private Serializable parseMessage(ProceedingJoinPoint pjp, ConsumeMqMessage consumeMqMessage) {
        Serializable message = null;
        Class<? extends Serializable> messageClass = consumeMqMessage.messageClass();
        if (!ConsumeMqMessage.DefaultMessageClass.class.equals(messageClass)) {
            message = findProvidedArgument(messageClass, pjp.getArgs());
        }
        if (StringUtils.isNotBlank(consumeMqMessage.messageExpression())) {
            Map<String, Object> root = Maps.newHashMap();
            root.put("args", pjp.getArgs());
            message = SpelUtils.parse(consumeMqMessage.messageExpression(), root,
                    Serializable.class);
        }
        if (StringUtils.isNotBlank(consumeMqMessage.messageProvider())) {
            MessageProvider<?> messageProvider =
                    getMessageProvider(consumeMqMessage.messageProvider());
            message = messageProvider.getMessage(consumeMqMessage, pjp);
        }
        if (message == null) {
            throw new MqException("unable to parse out the message! Please specify " +
                    "@ConsumeMqMessage property 'messageClass' or 'messageExpression' or " +
                    "'messageProvider' to help parse the message.");
        }
        return message;
    }

    private Object parseMessageId(ProceedingJoinPoint pjp, ConsumeMqMessage consumeMqMessage,
                                  Serializable message) {
        String messageIdExpression = consumeMqMessage.messageIdExpression();
        if (StringUtils.isBlank(messageIdExpression)) {
            throw new MqException("messageIdExpression is blank");
        }
        Map<String, Object> root = Maps.newHashMap();
        root.put("args", pjp.getArgs());
        root.put("message", message);
        return SpelUtils.parse(messageIdExpression, root);
    }

    protected void saveSuccessConsumedMessages(ConsumeMqMessage consumeMqMessage,
                                               ConsumedMessage consumedMessage) {
        saveConsumedMessages(consumeMqMessage, consumedMessage, true, null);
    }

    protected void saveFailConsumedMessages(ConsumeMqMessage consumeMqMessage,
                                            ConsumedMessage consumedMessage,
                                            Exception e) {
        saveConsumedMessages(consumeMqMessage, consumedMessage, false, e);
    }

    protected void saveConsumedMessages(ConsumeMqMessage consumeMqMessage,
                                        ConsumedMessage consumedMessage,
                                        boolean success, Exception e) {
        consumedMessage.setSuccess(success);
        if (e != null) {
            consumedMessage.setException(ExceptionUtils.getStackTrace(e));
        }
        consumedMessage.setCreateTime(new Date());
        getConsumerPersistence(consumeMqMessage).save(consumedMessage);
    }

    protected TransactionTemplate getTransactionTemplate(ConsumeMqMessage consumeMqMessage) {
        return SpringBeanUtils.getBean(consumeMqMessage.transactionManager(),
                TransactionTemplate.class);
    }

    protected MessageProvider<?> getMessageProvider(String messageProvider) {
        return SpringBeanUtils.getBean(messageProvider, MessageProvider.class);
    }

    protected ConsumerPersistence getConsumerPersistence(ConsumeMqMessage consumeMqMessage) {
        return SpringBeanUtils.getBean(consumeMqMessage.persistenceName(),
                ConsumerPersistence.class);
    }

    @Override
    public int getOrder() {
        return 10000;
    }

}
package org.xyattic.eventual.consistency.support.core.rabbit.provider.aop;

import com.google.common.collect.Lists;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ResolvableType;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;
import org.xyattic.boot.calkin.core.exception.BaseException;
import org.xyattic.boot.calkin.core.utils.SpringBeanUtils;
import org.xyattic.eventual.consistency.support.core.rabbit.provider.PendingMessage;
import org.xyattic.eventual.consistency.support.core.rabbit.provider.PendingMessageContextHolder;
import org.xyattic.eventual.consistency.support.core.rabbit.provider.Sender;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author wangxing
 * @create 2020/4/1
 */
@Aspect
@Slf4j
public class SendMqMessageAspect {

    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private Sender sender;

    @Pointcut("@within(sendMqMessage) || @annotation(sendMqMessage)")
    public void joinPoint(SendMqMessage sendMqMessage) {
    }

    @Around(value = "joinPoint(sendMqMessage)", argNames = "pjp,sendMqMessage")
    public final Object doAround(final ProceedingJoinPoint pjp, SendMqMessage sendMqMessage) throws Throwable {
        try {
            Object result = getTransactionTemplate(sendMqMessage).execute(status -> exec(pjp,
                    sendMqMessage));

            sendMessages(PendingMessageContextHolder.get());

            return result;
        } finally {
            PendingMessageContextHolder.clear();
        }
    }

    protected void sendMessages(List<PendingMessage> pendingMessages) {
        sender.send(pendingMessages);
    }

    @SneakyThrows
    protected Object exec(ProceedingJoinPoint pjp, SendMqMessage sendMqMessage) {
        final Object result = pjp.proceed();
        savePendingMessages(parseMessages(result,
                ((MethodSignature) pjp.getSignature()).getMethod()), sendMqMessage);
        return result;
    }

    protected List<PendingMessage> parseMessages(Object returnVal, Method method) {
        if (returnVal instanceof Collection) {
            Collection collection = ((Collection) returnVal);
            Class<?> clz = ResolvableType.forMethodReturnType(method)
                    .getGeneric(0).getRawClass();
            if (PendingMessage.class.equals(clz)) {
                PendingMessageContextHolder.set(Lists.newArrayList(collection));
            }
        } else if (returnVal instanceof PendingMessage) {
            PendingMessageContextHolder.set(Collections.singletonList(((PendingMessage) returnVal)));
        }
        return PendingMessageContextHolder.get();
    }

    protected void savePendingMessages(List<PendingMessage> pendingMessages,
                                       SendMqMessage sendMqMessage) {
        if (!CollectionUtils.isEmpty(pendingMessages)) {
            MongoTemplate mongoTemplate = getMongoTemplate(sendMqMessage);
            pendingMessages.forEach(pendingMessage -> {
                pendingMessage.setMongoTemplate(sendMqMessage.mongoTemplate());
                pendingMessage.setTransactionManager(sendMqMessage.transactionManager());
            });
            mongoTemplate.insertAll(pendingMessages);
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
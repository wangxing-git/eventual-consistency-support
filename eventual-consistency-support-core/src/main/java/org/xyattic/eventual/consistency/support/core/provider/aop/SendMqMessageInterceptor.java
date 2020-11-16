package org.xyattic.eventual.consistency.support.core.provider.aop;

import com.google.common.collect.Lists;
import lombok.SneakyThrows;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ResolvableType;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;
import org.xyattic.eventual.consistency.support.core.aop.support.AnnotationMethodInterceptor;
import org.xyattic.eventual.consistency.support.core.persistence.ProviderPersistence;
import org.xyattic.eventual.consistency.support.core.provider.PendingMessage;
import org.xyattic.eventual.consistency.support.core.provider.PendingMessageContextHolder;
import org.xyattic.eventual.consistency.support.core.sender.Sender;
import org.xyattic.eventual.consistency.support.core.utils.SpringBeanUtils;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * @author wangxing
 * @create 2020/6/25
 */
public class SendMqMessageInterceptor extends AnnotationMethodInterceptor<SendMqMessage> {

    @Autowired
    private Sender sender;

    @Override
    protected Object doInvoke(SendMqMessage sendMqMessage, ProceedingJoinPoint pjp) {
        try {
            Object result =
                    getTransactionTemplate(sendMqMessage).execute(status -> doInTransaction(pjp,
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
    protected Object doInTransaction(ProceedingJoinPoint pjp, SendMqMessage sendMqMessage) {
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
            pendingMessages.forEach(pendingMessage -> {
                pendingMessage.setPersistenceName(sendMqMessage.persistenceName());
                pendingMessage.setTransactionManager(sendMqMessage.transactionManager());
                pendingMessage.setCreateTime(new Date());
            });
            getProviderPersistence(sendMqMessage).save(pendingMessages);
        }
    }

    protected TransactionTemplate getTransactionTemplate(SendMqMessage sendMqMessage) {
        return SpringBeanUtils.getBean(sendMqMessage.transactionManager(),
                TransactionTemplate.class);
    }

    protected ProviderPersistence getProviderPersistence(SendMqMessage sendMqMessage) {
        return SpringBeanUtils.getBean(sendMqMessage.persistenceName(), ProviderPersistence.class);
    }

    @Override
    public int getOrder() {
        return 0;
    }

}
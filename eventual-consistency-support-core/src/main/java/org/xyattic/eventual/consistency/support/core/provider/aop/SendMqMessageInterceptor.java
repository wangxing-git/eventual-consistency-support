package org.xyattic.eventual.consistency.support.core.provider.aop;

import com.google.common.collect.Lists;
import lombok.SneakyThrows;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.aop.aspectj.MethodInvocationProceedingJoinPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;
import org.xyattic.eventual.consistency.support.core.exception.MqException;
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
public class SendMqMessageInterceptor implements MethodInterceptor, Ordered {

    @Autowired
    private Sender sender;

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        try {
            if (!(invocation instanceof ProxyMethodInvocation)) {
                throw new IllegalStateException("MethodInvocation is not a Spring " +
                        "ProxyMethodInvocation: " + invocation);
            }
            ProxyMethodInvocation pmi = (ProxyMethodInvocation) invocation;
            ProceedingJoinPoint pjp = lazyGetProceedingJoinPoint(pmi);
            SendMqMessage sendMqMessage =
                    AnnotatedElementUtils.getMergedAnnotation(invocation.getMethod(),
                            SendMqMessage.class);

            Object result = getTransactionTemplate(sendMqMessage).execute(status -> exec(pjp,
                    sendMqMessage));

            sendMessages(PendingMessageContextHolder.get());

            return result;
        } finally {
            PendingMessageContextHolder.clear();
        }
    }

    /**
     * Return the ProceedingJoinPoint for the current invocation,
     * instantiating it lazily if it hasn't been bound to the thread already.
     *
     * @param rmi the current Spring AOP ReflectiveMethodInvocation,
     *            which we'll use for attribute binding
     * @return the ProceedingJoinPoint to make available to advice methods
     */
    protected ProceedingJoinPoint lazyGetProceedingJoinPoint(ProxyMethodInvocation rmi) {
        return new MethodInvocationProceedingJoinPoint(rmi);
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
            pendingMessages.forEach(pendingMessage -> {
                pendingMessage.setPersistenceName(sendMqMessage.persistenceName());
                pendingMessage.setTransactionManager(sendMqMessage.transactionManager());
                pendingMessage.setCreateTime(new Date());
            });
            getProviderPersistence(sendMqMessage).save(pendingMessages);
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

    @Override
    public int getOrder() {
        return 0;
    }

}
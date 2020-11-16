package org.xyattic.eventual.consistency.support.core.consumer.aop;

import org.aspectj.lang.ProceedingJoinPoint;

import java.io.Serializable;

/**
 * @author wangxing
 * @create 2020/8/24
 */
public interface MessageProvider<T extends Serializable> {

    T getMessage(ConsumeMqMessage consumeMqMessage, Object[] args);

    default T getMessage(ConsumeMqMessage consumeMqMessage, ProceedingJoinPoint pjp) {
        return getMessage(consumeMqMessage, pjp.getArgs());
    }

}

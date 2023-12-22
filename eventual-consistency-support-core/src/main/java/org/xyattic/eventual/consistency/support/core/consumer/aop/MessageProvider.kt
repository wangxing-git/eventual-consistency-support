package org.xyattic.eventual.consistency.support.core.consumer.aop

import org.aspectj.lang.ProceedingJoinPoint
import org.xyattic.eventual.consistency.support.core.consumer.AbstractMessage

/**
 * @author wangxing
 * @create 2020/8/24
 */
interface MessageProvider<T : AbstractMessage?> {

    fun getMessage(consumeMqMessage: ConsumeMqMessage?, args: Array<Any?>?): T

    fun getMessage(consumeMqMessage: ConsumeMqMessage?, pjp: ProceedingJoinPoint): T {
        return getMessage(consumeMqMessage, pjp.args)
    }

}
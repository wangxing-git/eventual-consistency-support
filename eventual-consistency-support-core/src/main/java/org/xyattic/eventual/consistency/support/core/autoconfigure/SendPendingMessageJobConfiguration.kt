package org.xyattic.eventual.consistency.support.core.autoconfigure

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableScheduling
import org.xyattic.eventual.consistency.support.core.job.ReactiveSendPendingMessageJob
import org.xyattic.eventual.consistency.support.core.job.SendPendingMessageJob

/**
 * @author wangxing
 * @create 2020/4/9
 */
@EnableScheduling
class SendPendingMessageJobConfiguration {

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    fun sendPendingMessageJob(): SendPendingMessageJob {
        return SendPendingMessageJob()
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    fun reactiveSendPendingMessageJob(): ReactiveSendPendingMessageJob {
        return ReactiveSendPendingMessageJob()
    }

}
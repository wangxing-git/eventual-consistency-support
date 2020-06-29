package org.xyattic.eventual.consistency.support.core.autoconfigure;

import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.xyattic.eventual.consistency.support.core.job.SendPendingMessageJob;

/**
 * @author wangxing
 * @create 2020/4/9
 */
@EnableScheduling
public class SendPendingMessageJobConfiguration {

    @Bean
    public SendPendingMessageJob sendPendingMessageJob() {
        return new SendPendingMessageJob();
    }

}
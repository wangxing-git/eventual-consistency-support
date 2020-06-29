package org.xyattic.eventual.consistency.support.core.job;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.xyattic.eventual.consistency.support.core.lock.RedisLock;
import org.xyattic.eventual.consistency.support.core.persistence.ProviderPersistence;
import org.xyattic.eventual.consistency.support.core.sender.Sender;
import org.xyattic.eventual.consistency.support.core.utils.SpringBeanUtils;

import java.util.Date;

/**
 * @author wangxing
 * @create 2020/4/8
 */
@Slf4j
public class SendPendingMessageJob {

    @RedisLock
    @Scheduled(cron = "${common-mq.sendPendingMessageJob.cron:0 0/1 * * * ?}")
    public void check() {
        SpringBeanUtils.getBeanProvider(ProviderPersistence.class)
                .forEach(providerPersistence -> {
                    try {
                        providerPersistence.getPendingMessages(DateUtils.addMinutes(new Date(), -1))
                                .forEach(pendingMessage -> {
                                    SpringBeanUtils.getBean(Sender.class).send(pendingMessage);
                                });
                    } catch (Exception ignored) {
                        log.warn(ignored.getMessage(), ignored);
                    }
                });
    }

}
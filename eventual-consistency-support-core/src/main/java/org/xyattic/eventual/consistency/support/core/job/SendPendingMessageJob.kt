package org.xyattic.eventual.consistency.support.core.job

import org.apache.commons.lang3.time.DateUtils
import org.springframework.scheduling.annotation.Scheduled
import org.xyattic.eventual.consistency.support.core.lock.RedisLock
import org.xyattic.eventual.consistency.support.core.persistence.ProviderPersistence
import org.xyattic.eventual.consistency.support.core.provider.PendingMessage
import org.xyattic.eventual.consistency.support.core.sender.Sender
import org.xyattic.eventual.consistency.support.core.utils.SpringBeanUtils
import org.xyattic.eventual.consistency.support.core.utils.getLogger
import java.util.*

/**
 * @author wangxing
 * @create 2020/4/8
 */
open class SendPendingMessageJob {

    companion object {
        private val log = getLogger()
    }

    @RedisLock
    @Scheduled(cron = "\${eventual-consistency.sendPendingMessageJob.cron:0 0/1 * * * ?}")
    open fun checkPendingMessage() {
        SpringBeanUtils.getBeanProvider(ProviderPersistence::class.java)
                .forEach { providerPersistence: ProviderPersistence ->
                    try {
                        providerPersistence.getPendingMessages(DateUtils.addMinutes(Date(), -1))
                                .forEach { pendingMessage: PendingMessage -> SpringBeanUtils.getBean(Sender::class.java).send(pendingMessage) }
                    } catch (ignored: Exception) {
                        log.warn(ignored.message, ignored)
                    }
                }
    }
}
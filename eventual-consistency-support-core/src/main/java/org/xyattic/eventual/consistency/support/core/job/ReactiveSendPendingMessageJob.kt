package org.xyattic.eventual.consistency.support.core.job

import org.apache.commons.lang3.time.DateUtils
import org.springframework.scheduling.annotation.Scheduled
import org.xyattic.eventual.consistency.support.core.lock.RedisLock
import org.xyattic.eventual.consistency.support.core.persistence.reactive.ReactivePersistence
import org.xyattic.eventual.consistency.support.core.provider.PendingMessage
import org.xyattic.eventual.consistency.support.core.sender.ReactiveSender
import org.xyattic.eventual.consistency.support.core.utils.SpringBeanUtils
import org.xyattic.eventual.consistency.support.core.utils.getLogger
import reactor.core.publisher.Mono
import java.util.*

/**
 * @author wangxing
 * @create 2020/4/8
 */
open class ReactiveSendPendingMessageJob {

    companion object {
        private val log = getLogger()
    }

    @RedisLock
    @Scheduled(cron = "\${eventual-consistency.sendPendingMessageJob.cron:0 0/1 * * * ?}")
    open fun checkPendingMessage() {
        SpringBeanUtils.getBeanProvider(ReactivePersistence::class.java)
            .forEach { providerPersistence: ReactivePersistence ->
                providerPersistence.getPendingMessages(DateUtils.addMinutes(Date(), -1))
                    .flatMap { pendingMessage: PendingMessage ->
                        SpringBeanUtils.getBean(ReactiveSender::class.java).send(pendingMessage)
                            .onErrorResume {
                                log.warn(it.message, it)
                                Mono.empty()
                            }
                    }
                    .then()
                    .block()
            }
    }
}
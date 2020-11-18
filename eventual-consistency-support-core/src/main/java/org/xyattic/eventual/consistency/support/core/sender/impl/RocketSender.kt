package org.xyattic.eventual.consistency.support.core.sender.impl

import org.apache.rocketmq.client.producer.SendStatus
import org.apache.rocketmq.spring.core.RocketMQTemplate
import org.apache.rocketmq.spring.support.RocketMQHeaders
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.support.GenericMessage
import org.xyattic.eventual.consistency.support.core.persistence.ProviderPersistence
import org.xyattic.eventual.consistency.support.core.provider.PendingMessage
import org.xyattic.eventual.consistency.support.core.provider.enums.PendingMessageStatus
import org.xyattic.eventual.consistency.support.core.sender.Sender
import org.xyattic.eventual.consistency.support.core.utils.getLogger
import java.util.*

/**
 * @author wangxing
 * @create 2020/6/29
 */
open class RocketSender : AbstractSender(), Sender {

    companion object {
        private val log = getLogger()
    }

    @Autowired
    private lateinit var rocketMQTemplate: RocketMQTemplate

    @Autowired
    private lateinit var providerPersistence: ProviderPersistence

    override fun send(pendingMessage: PendingMessage) {
        getTransactionTemplate(pendingMessage)
                .execute {
                    var topicWithTags: String = pendingMessage.destination
                    if (pendingMessage.headers.containsKey(RocketMQHeaders.TAGS)) {
                        topicWithTags += ":${pendingMessage.headers[RocketMQHeaders.TAGS]}"
                    }
                    val sendResult = rocketMQTemplate.syncSend(topicWithTags,
                            GenericMessage(pendingMessage.body, pendingMessage.headers))
                    if (SendStatus.SEND_OK == sendResult.sendStatus) {
                        log.info("sent successfully: ${sendResult}")
                        providerPersistence.changePendingMessageStatus(pendingMessage.messageId,
                                PendingMessageStatus.HAS_BEEN_SENT, Date())
                    } else {
                        log.warn("failed to send: $sendResult")
                    }
                }

    }

}
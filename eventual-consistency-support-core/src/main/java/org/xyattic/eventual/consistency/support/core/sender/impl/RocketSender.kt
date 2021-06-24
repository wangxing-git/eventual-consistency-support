package org.xyattic.eventual.consistency.support.core.sender.impl

import org.apache.rocketmq.client.producer.SendStatus
import org.apache.rocketmq.spring.core.RocketMQTemplate
import org.apache.rocketmq.spring.support.RocketMQHeaders
import org.springframework.messaging.support.GenericMessage
import org.xyattic.eventual.consistency.support.core.provider.PendingMessage
import org.xyattic.eventual.consistency.support.core.provider.PendingMessageHeaders
import org.xyattic.eventual.consistency.support.core.utils.getLogger

/**
 * @author wangxing
 * @create 2020/6/29
 */
open class RocketSender(private val rocketMQTemplate: RocketMQTemplate) : AbstractSender() {

    companion object {
        private val log = getLogger()
    }


    override fun send(pendingMessage: PendingMessage) {
        getTransactionTemplate(pendingMessage)
            .execute {
                var topicWithTags: String = pendingMessage.destination
                if (pendingMessage.headers.containsKey(RocketMQHeaders.TAGS)) {
                    topicWithTags += ":${pendingMessage.headers[RocketMQHeaders.TAGS]}"
                }
                val sendResult =
                    if (pendingMessage.headers.containsKey(PendingMessageHeaders.partitionHeader)) {
                        val partitionKey =
                            pendingMessage.headers[PendingMessageHeaders.partitionHeader].toString()
                        log.info("send orderly message with '$partitionKey' for $pendingMessage")
                        rocketMQTemplate.syncSendOrderly(
                            topicWithTags,
                            GenericMessage(pendingMessage.body, pendingMessage.headers),
                            partitionKey
                        )
                    } else {
                        rocketMQTemplate.syncSend(
                            topicWithTags,
                            GenericMessage(pendingMessage.body, pendingMessage.headers)
                        )
                    }
                if (SendStatus.SEND_OK == sendResult.sendStatus) {
                    log.info("sent successfully: ${sendResult}")
                    getProviderPersistence(pendingMessage.persistenceName).sendSuccess(
                        pendingMessage.messageId,
                        sendResult.msgId
                    )
                } else {
                    log.warn("failed to send: $sendResult")
                }
            }

    }

}
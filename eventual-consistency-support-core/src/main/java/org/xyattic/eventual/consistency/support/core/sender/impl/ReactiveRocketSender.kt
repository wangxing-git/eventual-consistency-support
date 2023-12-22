package org.xyattic.eventual.consistency.support.core.sender.impl

import org.apache.rocketmq.client.producer.SendCallback
import org.apache.rocketmq.client.producer.SendResult
import org.apache.rocketmq.client.producer.SendStatus
import org.apache.rocketmq.spring.core.RocketMQTemplate
import org.apache.rocketmq.spring.support.RocketMQHeaders
import org.springframework.messaging.support.GenericMessage
import org.xyattic.eventual.consistency.support.core.provider.PendingMessage
import org.xyattic.eventual.consistency.support.core.provider.PendingMessageHeaders
import org.xyattic.eventual.consistency.support.core.utils.getLogger
import reactor.core.publisher.Mono
import reactor.core.publisher.MonoSink

/**
 * @author wangxing
 * @create 2020/6/29
 */
open class ReactiveRocketSender(private val rocketMQTemplate: RocketMQTemplate) :
    AbstractReactiveSender() {

    companion object {
        private val log = getLogger()
    }

    override fun send(pendingMessage: PendingMessage): Mono<Void> {
        return getTransactionalOperator(pendingMessage)
            .transactional(Mono.create { it: MonoSink<SendResult> ->
                var topicWithTags: String = pendingMessage.destination
                if (pendingMessage.headers.containsKey(RocketMQHeaders.TAGS)) {
                    topicWithTags += ":${pendingMessage.headers[RocketMQHeaders.TAGS]}"
                }
                val callback = object : SendCallback {
                    override fun onSuccess(sendResult: SendResult) {
                        it.success(sendResult)
                    }

                    override fun onException(e: Throwable) {
                        it.error(e)
                    }
                }
                if (pendingMessage.headers.containsKey(PendingMessageHeaders.partitionHeader)) {
                    val partitionKey =
                        pendingMessage.headers[PendingMessageHeaders.partitionHeader].toString()
                    log.info("send orderly message with '$partitionKey' for $pendingMessage")
                    rocketMQTemplate.asyncSendOrderly(
                        topicWithTags,
                        GenericMessage(pendingMessage.body, pendingMessage.headers),
                        partitionKey,
                        callback
                    )
                } else {
                    log.info("send message for $pendingMessage")
                    rocketMQTemplate.asyncSend(
                        topicWithTags,
                        GenericMessage(pendingMessage.body, pendingMessage.headers),
                        callback
                    )
                }
            }.flatMap { sendResult: SendResult ->
                if (SendStatus.SEND_OK == sendResult.sendStatus) {
                    log.info("sent successfully: ${sendResult}")
                    return@flatMap getReactiveProviderPersistence(pendingMessage.persistenceName)
                        .sendSuccess(
                            pendingMessage.messageId,
                            sendResult.msgId
                        )
                } else {
                    log.warn("failed to send: $sendResult")
                }
                Mono.empty<Void>()
            }).onErrorResume {
                log.error("failed to send: ${pendingMessage}", it)
                Mono.empty()
            }
    }

}
package org.xyattic.eventual.consistency.support.core.sender.impl

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.amqp.core.Message
import org.springframework.amqp.core.MessagePostProcessor
import org.springframework.amqp.core.MessageProperties
import org.springframework.amqp.rabbit.connection.CorrelationData
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import org.xyattic.eventual.consistency.support.core.constants.RabbitConstants
import org.xyattic.eventual.consistency.support.core.provider.PendingMessage
import org.xyattic.eventual.consistency.support.core.utils.RabbitUtils
import org.xyattic.eventual.consistency.support.core.utils.getLogger

/**
 * @author wangxing
 * @create 2020/4/1
 */
open class RabbitSender(private val rabbitTemplate: RabbitTemplate,
                        private var objectMapper: ObjectMapper) : AbstractSender(), InitializingBean {

    companion object {
        private val log = getLogger()
    }

    override fun send(pendingMessage: PendingMessage) {
        try {
            val correlationData = CorrelationData()
            correlationData.id = pendingMessage.messageId
            val bytes = objectMapper.writeValueAsBytes(pendingMessage.body)
            val properties = MessageProperties()
            properties.setHeader("pendingMessage", pendingMessage)
            val message = Message(bytes, properties)
            correlationData.returnedMessage = message
            val messagePostProcessor = MessagePostProcessor {
                it.messageProperties.messageId = pendingMessage.messageId
                it.messageProperties.appId =
                        pendingMessage.headers[RabbitConstants.APP_ID_HEADER].toString()
                it
            }
            rabbitTemplate.convertAndSend(
                    RabbitUtils.getExchange(pendingMessage.headers[RabbitConstants.EXCHANGE_HEADER].toString()),
                    RabbitUtils.getRoutingKey(pendingMessage.destination),
                    pendingMessage.body, messagePostProcessor, correlationData
            )
            log.info("message sent successfully: {}", pendingMessage)
        } catch (e: Exception) {
            log.warn("message sending failed, pendingMessage:$pendingMessage", e)
        }
    }

    override fun afterPropertiesSet() {
        rabbitTemplate.setConfirmCallback { correlationData1: CorrelationData, ack: Boolean, cause: String? ->
            if (ack) {
                changeMessageStatusSuccess(correlationData1)
            } else {
                log.warn("confirm message failed: {}, correlationData: {}", cause, correlationData1)
            }
        }
        rabbitTemplate.setReturnCallback { message1: Message?, replyCode: Int, replyText: String?, exchange: String?, routingKey: String? ->
            log.error(
                    "message sending failed: message: {}, replyCode: {}, replyText: {}, exchange: {}, routingKey: {}",
                    message1, replyCode, replyText, exchange, routingKey
            )
        }
    }

    protected fun changeMessageStatusSuccess(correlationData: CorrelationData) {
        val pendingMessage =
                correlationData.returnedMessage?.messageProperties?.getHeader<PendingMessage>(
                        "pendingMessage"
                )
        if (pendingMessage != null) {
            getTransactionTemplate(pendingMessage.transactionManager).executeWithoutResult {
                getProviderPersistence(pendingMessage.persistenceName)
                        .sendSuccess(pendingMessage.messageId, pendingMessage.messageId)
            }
        }
    }

}
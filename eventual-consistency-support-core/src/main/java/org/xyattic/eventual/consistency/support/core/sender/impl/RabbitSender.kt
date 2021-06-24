package org.xyattic.eventual.consistency.support.core.sender.impl

import com.alibaba.fastjson.JSON
import org.springframework.amqp.core.Message
import org.springframework.amqp.core.MessagePostProcessor
import org.springframework.amqp.core.MessageProperties
import org.springframework.amqp.rabbit.connection.CorrelationData
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.InitializingBean
import org.xyattic.eventual.consistency.support.core.constants.RabbitConstants
import org.xyattic.eventual.consistency.support.core.provider.PendingMessage
import org.xyattic.eventual.consistency.support.core.utils.RabbitUtils
import org.xyattic.eventual.consistency.support.core.utils.getLogger

/**
 * @author wangxing
 * @create 2020/4/1
 */
open class RabbitSender(private val rabbitTemplate: RabbitTemplate) : AbstractSender(),
    InitializingBean {

    companion object {
        private val log = getLogger()
    }

    override fun send(pendingMessage: PendingMessage) {
        try {
            val correlationData = CorrelationData()
            correlationData.id = pendingMessage.messageId
            val bytes = JSON.toJSONString(pendingMessage.body).toByteArray()
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
            log.info("成功发送消息:{}", pendingMessage)
        } catch (e: Exception) {
            log.warn("消息发送失败,pendingMessage:$pendingMessage", e)
        }
    }

    override fun afterPropertiesSet() {
        rabbitTemplate.setConfirmCallback { correlationData1: CorrelationData, ack: Boolean, cause: String? ->
            if (ack) {
                changeMessageStatusSuccess(correlationData1)
            } else {
                log.warn("确认消息失败:{},correlationData:{}", cause, correlationData1)
            }
        }
        rabbitTemplate.setReturnCallback { message1: Message?, replyCode: Int, replyText: String?, exchange: String?, routingKey: String? ->
            log.error(
                "消息发送失败:message:{},replyCode:{},replyText:{},exchange:{},routingKey:{}",
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
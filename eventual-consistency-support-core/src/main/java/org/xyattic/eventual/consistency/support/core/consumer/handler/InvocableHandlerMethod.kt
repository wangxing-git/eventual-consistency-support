package org.xyattic.eventual.consistency.support.core.consumer.handler

import com.alibaba.fastjson.JSONObject
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.exception.ExceptionUtils
import org.springframework.amqp.core.MessageDeliveryMode
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.dao.DuplicateKeyException
import org.springframework.lang.Nullable
import org.springframework.messaging.Message
import org.springframework.messaging.handler.HandlerMethod
import org.springframework.messaging.handler.invocation.InvocableHandlerMethod
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.util.ObjectUtils
import org.xyattic.eventual.consistency.support.core.consumer.ConsumedMessage
import org.xyattic.eventual.consistency.support.core.consumer.aop.RabbitConsumer
import org.xyattic.eventual.consistency.support.core.exception.MqException
import org.xyattic.eventual.consistency.support.core.persistence.ConsumerPersistence
import org.xyattic.eventual.consistency.support.core.utils.SpringBeanUtils
import org.xyattic.eventual.consistency.support.core.utils.getLogger
import java.lang.reflect.Method
import java.util.*

/**
 * @author wangxing
 * @create 2020/3/27
 */
@Deprecated("")
class InvocableHandlerMethod : InvocableHandlerMethod {

    companion object {
        private val log = getLogger()

        protected const val CONSUMED_COUNT_HEADER_NAME = "consumedCount"

        protected fun <T> findProvidedArgument(parameter: Class<T>,
                                               @Nullable vararg providedArgs: Any?): T? {
            if (!ObjectUtils.isEmpty(providedArgs)) {
                for (providedArg in providedArgs) {
                    if (parameter.isInstance(providedArg)) {
                        return parameter.cast(providedArg)
                    }
                }
            }
            return null
        }
    }

    protected var maxConsume = 10
    protected fun getTransactionTemplate(rabbitConsumer: RabbitConsumer): TransactionTemplate {
        return getBean(rabbitConsumer.transactionManager, TransactionTemplate::class.java)
    }

    protected fun getConsumerPersistence(rabbitConsumer: RabbitConsumer): ConsumerPersistence {
        return getBean(rabbitConsumer.persistenceName, ConsumerPersistence::class.java)
    }

    protected fun <T> getBean(beanName: String?, beanClass: Class<T>): T {
        val bean: T?
        if (StringUtils.isNotBlank(beanName)) {
            bean = SpringBeanUtils.getBean(beanName, beanClass)
        } else {
            bean = SpringBeanUtils.beanFactory.getBeanProvider(beanClass).ifUnique
            if (bean == null) {
                val beanNamesForType = SpringBeanUtils.beanFactory.getBeanNamesForType(beanClass)
                bean = if (beanNamesForType.size == 0) {
                    throw MqException(beanClass.simpleName + " bean not found in " +
                            "spring")
                } else if (beanNamesForType.size == 1) {
                    SpringBeanUtils.getBean(beanNamesForType[0])
                } else {
                    throw MqException(beanClass.simpleName + " bean found " + beanNamesForType.size + ", please specify a name")
                }
            }
        }
        return bean ?: throw MqException(beanClass.simpleName + " bean not found in " +
                "spring")
    }

    constructor(handlerMethod: HandlerMethod) : super(handlerMethod) {}
    constructor(bean: Any, method: Method) : super(bean, method) {}
    constructor(bean: Any, methodName: String, vararg parameterTypes: Class<*>?) : super(bean, methodName, *parameterTypes) {}

    override fun invoke(message: Message<*>?, vararg providedArgs: Any): Any? {
        val rabbitConsumer = AnnotatedElementUtils.findMergedAnnotation(method,
                RabbitConsumer::class.java)
        return if (rabbitConsumer != null) {
            val debugEnabled = log.isDebugEnabled
            val transactionTemplate = getTransactionTemplate(rabbitConsumer)
            val amqpMessage: org.springframework.amqp.core.Message = findProvidedArgument(org.springframework.amqp.core.Message::class.java, *providedArgs)
                    ?: throw IllegalStateException("message")
            val channel: Channel = findProvidedArgument(Channel::class.java,
                    *providedArgs) ?: throw IllegalStateException("channel")

            //设置每次从队列里取出一条处理
            channel.basicQos(1)
            try {
                val obj = transactionTemplate.execute { status: TransactionStatus? ->
                    invoke(message, rabbitConsumer, amqpMessage, providedArgs)
                }
                channel.basicAck(amqpMessage.messageProperties.deliveryTag, false)
                return obj
            } catch (d: DuplicateKeyException) {
                log.info("重复的消息,已忽略,message:{}", amqpMessage)
                //忽略重复的消息
                channel.basicAck(amqpMessage.messageProperties.deliveryTag, false)
            } catch (e: Exception) {
                log.warn("消费失败,message:$amqpMessage", e)
                val count = getConsumedCount(amqpMessage)
                if (count >= maxConsume) {
                    log.info("消费次数已达最大限制:{},将丢弃该消息message:{}", count, amqpMessage)
                    saveFailConsumedMessages(rabbitConsumer, amqpMessage, e)
                } else {
                    log.info("进行消息重发,message:{}", amqpMessage)
                    resend(amqpMessage, channel)
                }
                channel.basicAck(amqpMessage.messageProperties.deliveryTag, false)
            }
            null
        } else {
            super.invoke(message, *providedArgs)
        }
    }

    protected fun getConsumedCount(amqpMessage: org.springframework.amqp.core.Message): Int {
        return Optional.ofNullable(
                amqpMessage.messageProperties.getHeader<Any>(CONSUMED_COUNT_HEADER_NAME) as Int).orElse(1)
    }

    protected fun resend(amqpMessage: org.springframework.amqp.core.Message, channel: Channel) {
        val messageProperties = amqpMessage.messageProperties
        val count = getConsumedCount(amqpMessage)
        messageProperties.setHeader(CONSUMED_COUNT_HEADER_NAME, count + 1)
        val basicProperties = AMQP.BasicProperties(
                messageProperties.contentType,
                messageProperties.contentEncoding, messageProperties.headers,
                MessageDeliveryMode.toInt(messageProperties.receivedDeliveryMode),
                messageProperties.priority, messageProperties.correlationId,
                messageProperties.replyTo, messageProperties.expiration,
                messageProperties.messageId, messageProperties.timestamp,
                messageProperties.type, messageProperties.userId,
                messageProperties.appId, messageProperties.clusterId)
        channel.basicPublish(amqpMessage.messageProperties.receivedExchange,
                amqpMessage.messageProperties.receivedRoutingKey,
                basicProperties, amqpMessage.body)
    }

    protected operator fun invoke(message: Message<*>?, rabbitConsumer: RabbitConsumer,
                                  amqpMessage: org.springframework.amqp.core.Message,
                                  providedArgs: Array<Any?>): Any {
        saveSuccessConsumedMessages(rabbitConsumer, amqpMessage)
        return super.invoke(message, *providedArgs)
    }

    protected fun saveSuccessConsumedMessages(rabbitConsumer: RabbitConsumer,
                                              message: org.springframework.amqp.core.Message) {
        saveConsumedMessages(rabbitConsumer, message, true, null)
    }

    protected fun saveFailConsumedMessages(rabbitConsumer: RabbitConsumer,
                                           message: org.springframework.amqp.core.Message,
                                           e: Exception?) {
        saveConsumedMessages(rabbitConsumer, message, false, e)
    }

    protected fun saveConsumedMessages(rabbitConsumer: RabbitConsumer,
                                       message: org.springframework.amqp.core.Message,
                                       success: Boolean, e: Exception?) {
        val m = JSONObject.parseObject(JSONObject.toJSONString(message))
        try {
            m["body"] = JSONObject.parse(message.body)
        } catch (ignored: Exception) {
            m["body"] = message.body
        }
        val consumedMessage = ConsumedMessage(message.messageProperties.messageId, m, success = success)
        consumedMessage.consumeTime = Date()
        if (e != null) {
            consumedMessage.exception = ExceptionUtils.getStackTrace(e)
        }
        getConsumerPersistence(rabbitConsumer).save(consumedMessage)
    }

}
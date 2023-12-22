package org.xyattic.eventual.consistency.support.core.autoconfigure

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.rocketmq.spring.core.RocketMQTemplate
import org.springframework.amqp.rabbit.annotation.RabbitListenerConfigurer
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistrar
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.*
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.core.convert.support.DefaultConversionService
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory
import org.xyattic.eventual.consistency.support.core.sender.ReactiveSender
import org.xyattic.eventual.consistency.support.core.sender.Sender
import org.xyattic.eventual.consistency.support.core.sender.impl.RabbitSender
import org.xyattic.eventual.consistency.support.core.sender.impl.ReactiveRocketSender
import org.xyattic.eventual.consistency.support.core.sender.impl.RocketSender
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * @author wangxing
 * @create 2020/4/8
 */
@Configuration
class SenderConfiguration {

    @Configuration
    @ConditionalOnProperty(name = ["eventual-consistency.sender-type"], havingValue = "rabbitmq", matchIfMissing = true)
    @ConditionalOnClass(RabbitTemplate::class)
    internal class RabbitConfiguration {

        @Autowired
        private lateinit var beanFactory: BeanFactory

        @Bean
        @ConditionalOnMissingBean
        fun rabbitSender(rabbitTemplate: RabbitTemplate, objectMapper: ObjectMapper): Sender {
            return RabbitSender(rabbitTemplate, objectMapper)
        }

        @Bean
        @ConditionalOnMissingBean
        fun jackson2JsonMessageConverter(): Jackson2JsonMessageConverter {
            return Jackson2JsonMessageConverter()
        }

        @Configuration
        internal open class RabbitListenerConfigurerImpl : RabbitListenerConfigurer {

            override fun configureRabbitListeners(registrar: RabbitListenerEndpointRegistrar) {
                //@Deprecated
//            registrar.setMessageHandlerMethodFactory(createDefaultMessageHandlerMethodFactory());
            }

        }

    }

    @Configuration
    @ConditionalOnProperty(name = ["eventual-consistency.sender-type"], havingValue = "rocketmq", matchIfMissing = true)
    @ConditionalOnClass(RocketMQTemplate::class)
    @ConditionalOnBean(RocketMQTemplate::class)
    internal class RocketConfiguration {

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
        fun rocketSender(rocketMQTemplate: RocketMQTemplate): Sender {
            return RocketSender(rocketMQTemplate)
        }

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
        fun reactiveRocketSender(rocketMQTemplate: RocketMQTemplate): ReactiveSender {
            return ReactiveRocketSender(rocketMQTemplate)
        }

    }
}
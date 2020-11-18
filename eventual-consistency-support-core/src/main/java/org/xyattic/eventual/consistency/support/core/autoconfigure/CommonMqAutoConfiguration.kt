package org.xyattic.eventual.consistency.support.core.autoconfigure

import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.core.annotation.Order
import org.xyattic.eventual.consistency.support.core.aop.support.AnnotationClassOrMethodBeanFactoryPointcutAdvisor
import org.xyattic.eventual.consistency.support.core.consumer.aop.ConsumeMqMessageInterceptor
import org.xyattic.eventual.consistency.support.core.properties.EventualConsistencyProperties
import org.xyattic.eventual.consistency.support.core.provider.aop.SendMqMessageInterceptor

/**
 * @author wangxing
 * @create 2020/4/8
 */
@Configuration
@Order(0)
@AutoConfigureAfter(value = [MongoDataAutoConfiguration::class, RabbitAutoConfiguration::class, RedisAutoConfiguration::class, JdbcTemplateAutoConfiguration::class])
@Import(SenderConfiguration::class, DatabaseConfiguration::class, RedisLockConfiguration::class)
@EnableConfigurationProperties(value = [EventualConsistencyProperties::class])
@ConditionalOnProperty(name = ["eventual-consistency.enabled"], matchIfMissing = true)
class CommonMqAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = ["sendMqMessageInterceptorAdvisor"])
    fun sendMqMessageInterceptorAdvisor(
            sendMqMessageInterceptor: SendMqMessageInterceptor): AnnotationClassOrMethodBeanFactoryPointcutAdvisor {
        return AnnotationClassOrMethodBeanFactoryPointcutAdvisor(sendMqMessageInterceptor)
    }

    @Bean
    @ConditionalOnMissingBean(name = ["consumeMqMessageInterceptorAdvisor"])
    fun consumeMqMessageInterceptorAdvisor(
            consumeMqMessageInterceptor: ConsumeMqMessageInterceptor): AnnotationClassOrMethodBeanFactoryPointcutAdvisor {
        return AnnotationClassOrMethodBeanFactoryPointcutAdvisor(consumeMqMessageInterceptor)
    }

    @Bean
    @ConditionalOnMissingBean
    fun sendMqMessageInterceptor(): SendMqMessageInterceptor {
        return SendMqMessageInterceptor()
    }

    @Bean
    @ConditionalOnMissingBean
    fun consumeMqMessageInterceptor(): ConsumeMqMessageInterceptor {
        return ConsumeMqMessageInterceptor()
    }

}
package org.xyattic.eventual.consistency.support.core.autoconfigure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.xyattic.eventual.consistency.support.core.aop.support.AnnotationClassOrMethodBeanFactoryPointcutAdvisor;
import org.xyattic.eventual.consistency.support.core.consumer.aop.ConsumeMqMessageInterceptor;
import org.xyattic.eventual.consistency.support.core.properties.EventualConsistency;
import org.xyattic.eventual.consistency.support.core.provider.aop.SendMqMessageInterceptor;

/**
 * @author wangxing
 * @create 2020/4/8
 */
@Slf4j
@Configuration
@Order(0)
@AutoConfigureAfter(value = {MongoDataAutoConfiguration.class, RabbitAutoConfiguration.class,
        RedisAutoConfiguration.class, JdbcTemplateAutoConfiguration.class})
@Import({SenderConfiguration.class, DatabaseConfiguration.class, RedisLockConfiguration.class})
@EnableConfigurationProperties(EventualConsistency.class)
@ConditionalOnProperty(name = "eventual-consistency.enabled", matchIfMissing = true)
public class CommonMqAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "sendMqMessageInterceptorAdvisor")
    public AnnotationClassOrMethodBeanFactoryPointcutAdvisor sendMqMessageInterceptorAdvisor(
            SendMqMessageInterceptor sendMqMessageInterceptor) {
        return new AnnotationClassOrMethodBeanFactoryPointcutAdvisor(sendMqMessageInterceptor);
    }

    @Bean
    @ConditionalOnMissingBean(name = "consumeMqMessageInterceptorAdvisor")
    public AnnotationClassOrMethodBeanFactoryPointcutAdvisor consumeMqMessageInterceptorAdvisor(
            ConsumeMqMessageInterceptor consumeMqMessageInterceptor) {
        return new AnnotationClassOrMethodBeanFactoryPointcutAdvisor(consumeMqMessageInterceptor);
    }

    @Bean
    @ConditionalOnMissingBean
    public SendMqMessageInterceptor sendMqMessageInterceptor() {
        return new SendMqMessageInterceptor();
    }

    @Bean
    @ConditionalOnMissingBean
    public ConsumeMqMessageInterceptor consumeMqMessageInterceptor() {
        return new ConsumeMqMessageInterceptor();
    }

}
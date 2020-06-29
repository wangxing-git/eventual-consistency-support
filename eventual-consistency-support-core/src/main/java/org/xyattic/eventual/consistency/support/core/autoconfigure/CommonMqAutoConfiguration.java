package org.xyattic.eventual.consistency.support.core.autoconfigure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.xyattic.eventual.consistency.support.core.provider.aop.BeanFactorySendMqMessageAdvisor;
import org.xyattic.eventual.consistency.support.core.provider.aop.SendMqMessageInterceptor;

/**
 * @author wangxing
 * @create 2020/4/8
 */
@Slf4j
@Configuration
@Order(0)
@AutoConfigureAfter({MongoDataAutoConfiguration.class, RabbitAutoConfiguration.class,
        RedisAutoConfiguration.class})
@Import({SenderConfiguration.class, DatabaseConfiguration.class, RedisLockConfiguration.class})
public class CommonMqAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public BeanFactorySendMqMessageAdvisor beanFactorySendMqMessageAdvisor(SendMqMessageInterceptor sendMqMessageInterceptor) {
        BeanFactorySendMqMessageAdvisor beanFactorySendMqMessageAdvisor =
                new BeanFactorySendMqMessageAdvisor();
        beanFactorySendMqMessageAdvisor.setAdvice(sendMqMessageInterceptor);
        beanFactorySendMqMessageAdvisor.setOrder(sendMqMessageInterceptor.getOrder());
        return beanFactorySendMqMessageAdvisor;
    }

    @Bean
    @ConditionalOnMissingBean
    public SendMqMessageInterceptor sendMqMessageInterceptor() {
        return new SendMqMessageInterceptor();
    }

}
package org.xyattic.eventual.consistency.support.example.provider.config;

import lombok.SneakyThrows;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.autoconfigure.amqp.RabbitRetryTemplateCustomizer;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.xyattic.eventual.consistency.support.core.annotation.EnableReSendJob;
import org.xyattic.eventual.consistency.support.core.utils.RabbitUtils;

import java.time.Duration;
import java.util.stream.Collectors;

/**
 * @author wangxing
 * @create 2020/3/23
 */
@Configuration
@EnableRabbit
@EnableReSendJob
public class RabbitConfig {

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public RabbitTemplate rabbitTemplate(RabbitProperties properties,
                                         ObjectProvider<MessageConverter> messageConverter,
                                         ObjectProvider<RabbitRetryTemplateCustomizer> retryTemplateCustomizers,
                                         ConnectionFactory connectionFactory) {
        PropertyMapper map = PropertyMapper.get();
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        messageConverter.ifUnique(template::setMessageConverter);
        template.setMandatory(determineMandatoryFlag(properties));
        RabbitProperties.Template templateProperties = properties.getTemplate();
        if (templateProperties.getRetry().isEnabled()) {
            template.setRetryTemplate(
                    new RetryTemplateFactory(retryTemplateCustomizers.orderedStream().collect(Collectors.toList()))
                            .createRetryTemplate(templateProperties.getRetry(),
                                    RabbitRetryTemplateCustomizer.Target.SENDER));
        }
        map.from(templateProperties::getReceiveTimeout).whenNonNull().as(Duration::toMillis)
                .to(template::setReceiveTimeout);
        map.from(templateProperties::getReplyTimeout).whenNonNull().as(Duration::toMillis)
                .to(template::setReplyTimeout);
        map.from(templateProperties::getExchange).to(template::setExchange);
        map.from(templateProperties::getRoutingKey).to(template::setRoutingKey);
        map.from(templateProperties::getDefaultReceiveQueue).whenNonNull().to(template::setDefaultReceiveQueue);
        return template;
    }

    private boolean determineMandatoryFlag(RabbitProperties properties) {
        Boolean mandatory = properties.getTemplate().getMandatory();
        return (mandatory != null) ? mandatory : properties.isPublisherReturns();
    }

    /**
     * 声明直连交换机绑定
     */
    @Bean
    public Binding bindingCheckPaymentSuccess() {
        return BindingBuilder.bind(testQueue()).to(testExchange()).with(
                RabbitUtils.getRoutingKey("test-k"));
    }

    @Bean
    public Binding bindingCheckPaymentSuccess2() {
        return BindingBuilder.bind(testQueue2()).to(testExchange()).with(
                RabbitUtils.getRoutingKey("test-k2"));
    }

    @Bean
    public DirectExchange testExchange() {
        return new DirectExchange(RabbitUtils.getExchange("test-e"));
    }

//    @Bean
//    public DirectExchange testExchange2() {
//        return new DirectExchange(RabbitUtils.getExchange("test-e2"));
//    }

    @Bean
    @SneakyThrows
    public Queue testQueue() {
        return new Queue(RabbitUtils.getQueue("test-q10"));
    }

    @Bean
    @SneakyThrows
    public Queue testQueue2() {
        return new Queue(RabbitUtils.getQueue("test-q20"));
    }

}
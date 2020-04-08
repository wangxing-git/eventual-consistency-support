package org.xyattic.eventual.consistency.support.core.rabbit.config;

import lombok.SneakyThrows;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.RabbitListenerConfigurer;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistrar;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.autoconfigure.amqp.RabbitRetryTemplateCustomizer;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.xyattic.boot.calkin.core.utils.SpringBeanUtils;
import org.xyattic.eventual.consistency.support.core.rabbit.RabbitUtils;
import org.xyattic.eventual.consistency.support.core.rabbit.handler.DefaultMessageHandlerMethodFactory;
import org.xyattic.eventual.consistency.support.core.rabbit.provider.PendingMessage;
import org.xyattic.eventual.consistency.support.core.rabbit.provider.RabbitSender;
import org.xyattic.eventual.consistency.support.core.rabbit.provider.Sender;
import org.xyattic.eventual.consistency.support.core.rabbit.provider.aop.SendMqMessageAspect;
import org.xyattic.eventual.consistency.support.core.rabbit.provider.enums.PendingMessageStatus;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.stream.Collectors;

/**
 * @author wangxing
 * @create 2020/3/23
 */
@Configuration
@EnableRabbit
@EnableScheduling
public class RabbitConfig implements RabbitListenerConfigurer {

    @Autowired
    private AmqpAdmin amqpAdmin;
    @Autowired
    private BeanFactory beanFactory;

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

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 声明对账队列与直连交换机绑定
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
        return new Queue(RabbitUtils.getQueue("test-q"));
    }

    @Bean
    @SneakyThrows
    public Queue testQueue2() {
        return new Queue(RabbitUtils.getQueue("test-q2"));
    }

//    @Bean
//    public RabbitConsumerAspect rabbitConsumerAspect() {
//        return new RabbitConsumerAspect();
//    }

    @Override
    public void configureRabbitListeners(RabbitListenerEndpointRegistrar registrar) {
        registrar.setMessageHandlerMethodFactory(createDefaultMessageHandlerMethodFactory());
    }

    @Bean
    public SendMqMessageAspect sendMqMessageAspect() {
        return new SendMqMessageAspect();
    }

    @Bean
    public Sender sender() {
        return new RabbitSender();
    }

    @Scheduled(cron = "0 0/1 * * * ?")
    public void check() {
        SpringBeanUtils.getBeanProvider(MongoTemplate.class)
                .forEach(mongoTemplate -> {
                    try {
                        //todo 分页
                        mongoTemplate.find(Query.query(Criteria.where("status").is(PendingMessageStatus.PENDING)), PendingMessage.class)
                                .forEach(pendingMessage -> {
                                    sender().send(pendingMessage);
                                });
                    } catch (Exception ignored) {
                        ignored.printStackTrace();
                    }
                });
    }

    private MessageHandlerMethodFactory createDefaultMessageHandlerMethodFactory() {
        DefaultMessageHandlerMethodFactory defaultFactory =
                new DefaultMessageHandlerMethodFactory();
        defaultFactory.setBeanFactory(beanFactory);
        DefaultConversionService conversionService = new DefaultConversionService();
        conversionService.addConverter(new BytesToStringConverter(StandardCharsets.UTF_8));
        defaultFactory.setConversionService(conversionService);
        defaultFactory.afterPropertiesSet();
        return defaultFactory;
    }

    private static class BytesToStringConverter implements Converter<byte[], String> {


        private final Charset charset;

        BytesToStringConverter(Charset charset) {
            this.charset = charset;
        }

        @Override
        public String convert(byte[] source) {
            return new String(source, this.charset);
        }

    }

}
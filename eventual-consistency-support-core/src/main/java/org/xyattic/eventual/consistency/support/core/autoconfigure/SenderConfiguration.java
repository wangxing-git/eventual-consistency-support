package org.xyattic.eventual.consistency.support.core.autoconfigure;

import org.springframework.amqp.rabbit.annotation.RabbitListenerConfigurer;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistrar;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;
import org.xyattic.eventual.consistency.support.core.consumer.handler.DefaultMessageHandlerMethodFactory;
import org.xyattic.eventual.consistency.support.core.sender.Sender;
import org.xyattic.eventual.consistency.support.core.sender.impl.RabbitSender;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * @author wangxing
 * @create 2020/4/8
 */
@Configuration
public class SenderConfiguration {

    @Configuration
    @ConditionalOnClass(RabbitTemplate.class)
    public static class RabbitConfiguration implements RabbitListenerConfigurer {

        @Autowired
        private BeanFactory beanFactory;

        @Bean
        @ConditionalOnMissingBean
        public Sender rabbitSender() {
            return new RabbitSender();
        }

        @Bean
        @ConditionalOnMissingBean
        public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
            return new Jackson2JsonMessageConverter();
        }

        @Override
        public void configureRabbitListeners(RabbitListenerEndpointRegistrar registrar) {
            registrar.setMessageHandlerMethodFactory(createDefaultMessageHandlerMethodFactory());
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

}
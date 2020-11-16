package org.xyattic.eventual.consistency.support.example.consumer.controller;

import org.xyattic.eventual.consistency.support.core.consumer.aop.ConsumeMqMessage;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author wangxing
 * @create 2020/11/12
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ConsumeMqMessage(messageClass = TestMessage.class, messageIdExpression = "#{message.eventId}")
public @interface ConsumeTestMessage {
}

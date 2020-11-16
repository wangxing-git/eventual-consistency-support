package org.xyattic.eventual.consistency.support.core.consumer.aop;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author wangxing
 * @create 2020/3/27
 * use {@link ConsumeMqMessage}
 */
@Deprecated
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface RabbitConsumer {

    String persistenceName() default "";

    String transactionManager() default "";

}

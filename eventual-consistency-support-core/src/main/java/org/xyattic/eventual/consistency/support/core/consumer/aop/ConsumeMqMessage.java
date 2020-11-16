package org.xyattic.eventual.consistency.support.core.consumer.aop;

import java.io.Serializable;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author wangxing
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface ConsumeMqMessage {

    String persistenceName() default "";

    String transactionManager() default "";

    Class<? extends Serializable> messageClass() default DefaultMessageClass.class;

    String messageExpression() default "";

    String messageProvider() default "";

    String messageIdExpression();

    class DefaultMessageClass implements Serializable {
    }

}

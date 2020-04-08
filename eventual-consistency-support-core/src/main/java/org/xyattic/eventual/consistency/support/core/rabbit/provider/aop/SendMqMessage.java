package org.xyattic.eventual.consistency.support.core.rabbit.provider.aop;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author wangxing
 * @create 2020/4/1
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface SendMqMessage {

    String mongoTemplate() default "";

    String transactionManager() default "";

}

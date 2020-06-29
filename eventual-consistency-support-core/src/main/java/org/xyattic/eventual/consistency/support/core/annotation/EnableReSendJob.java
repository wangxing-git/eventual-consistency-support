package org.xyattic.eventual.consistency.support.core.annotation;

import org.springframework.context.annotation.Import;
import org.xyattic.eventual.consistency.support.core.autoconfigure.SendPendingMessageJobConfiguration;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author wangxing
 * @create 2020/3/27
 */
@Target({ElementType.ANNOTATION_TYPE, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import(SendPendingMessageJobConfiguration.class)
public @interface EnableReSendJob {

}

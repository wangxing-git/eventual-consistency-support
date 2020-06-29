package org.xyattic.eventual.consistency.support.core.provider.aop;

import org.springframework.aop.Pointcut;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.support.AbstractBeanFactoryPointcutAdvisor;

/**
 * @author wangxing
 * @create 2020/6/25
 */
public class BeanFactorySendMqMessageAdvisor extends AbstractBeanFactoryPointcutAdvisor {

    public BeanFactorySendMqMessageAdvisor() {
    }

    @Override
    public Pointcut getPointcut() {
        AspectJExpressionPointcut aspectJExpressionPointcut = new AspectJExpressionPointcut();
        aspectJExpressionPointcut.setParameterNames("sendMqMessage");
        aspectJExpressionPointcut.setParameterTypes(SendMqMessage.class);
        aspectJExpressionPointcut.setExpression("@within(sendMqMessage) || @annotation(sendMqMessage)");
        return aspectJExpressionPointcut;
    }

}
package org.xyattic.eventual.consistency.support.core.consumer.handler;

import org.springframework.messaging.Message;
import org.springframework.messaging.handler.HandlerMethod;
import org.springframework.messaging.handler.invocation.InvocableHandlerMethod;

import java.lang.reflect.Method;

/**
 * @author wangxing
 * @create 2020/4/14
 */
public class DelegateInvocableHandlerMethod extends InvocableHandlerMethod {

    private InvocableHandlerMethod invocableHandlerMethod;

    public DelegateInvocableHandlerMethod(HandlerMethod handlerMethod) {
        super(handlerMethod);
        this.invocableHandlerMethod = new InvocableHandlerMethod(handlerMethod);
    }

    public DelegateInvocableHandlerMethod(Object bean, Method method) {
        super(bean, method);
        this.invocableHandlerMethod = new InvocableHandlerMethod(bean, method);
    }

    public DelegateInvocableHandlerMethod(Object bean, String methodName,
                                          Class<?>... parameterTypes) throws NoSuchMethodException {
        super(bean, methodName, parameterTypes);
        this.invocableHandlerMethod = new InvocableHandlerMethod(bean, methodName, parameterTypes);
    }

    @Override
    public Object invoke(Message<?> message, Object... providedArgs) throws Exception {
        return invocableHandlerMethod.invoke(message, providedArgs);
    }

}
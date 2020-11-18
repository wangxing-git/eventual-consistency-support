package org.xyattic.eventual.consistency.support.core.consumer.handler

import org.springframework.messaging.Message
import org.springframework.messaging.handler.HandlerMethod
import org.springframework.messaging.handler.invocation.InvocableHandlerMethod
import java.lang.reflect.Method

/**
 * @author wangxing
 * @create 2020/4/14
 */
@Deprecated("")
class DelegateInvocableHandlerMethod : InvocableHandlerMethod {
    private var invocableHandlerMethod: InvocableHandlerMethod

    constructor(handlerMethod: HandlerMethod) : super(handlerMethod) {
        invocableHandlerMethod = InvocableHandlerMethod(handlerMethod)
    }

    constructor(bean: Any, method: Method) : super(bean, method) {
        invocableHandlerMethod = InvocableHandlerMethod(bean, method)
    }

    constructor(bean: Any, methodName: String,
                vararg parameterTypes: Class<*>?) : super(bean, methodName, *parameterTypes) {
        invocableHandlerMethod = InvocableHandlerMethod(bean, methodName, *parameterTypes)
    }

    override fun invoke(message: Message<*>, vararg providedArgs: Any): Any? {
        return invocableHandlerMethod.invoke(message, *providedArgs)
    }
}
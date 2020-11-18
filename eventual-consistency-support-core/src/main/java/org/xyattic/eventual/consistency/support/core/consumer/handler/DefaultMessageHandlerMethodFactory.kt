/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.xyattic.eventual.consistency.support.core.consumer.handler

import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.BeanFactoryAware
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.core.convert.ConversionService
import org.springframework.format.support.DefaultFormattingConversionService
import org.springframework.lang.Nullable
import org.springframework.messaging.converter.GenericMessageConverter
import org.springframework.messaging.converter.MessageConverter
import org.springframework.messaging.handler.annotation.support.*
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolverComposite
import org.springframework.util.Assert
import org.springframework.validation.Validator
import java.lang.reflect.Method
import java.util.*

/**
 * The default [MessageHandlerMethodFactory] implementation creating an
 * [InvocableHandlerMethod] with the necessary
 * [HandlerMethodArgumentResolver] instances to detect and process
 * most of the use cases defined by
 * [MessageMapping][org.springframework.messaging.handler.annotation.MessageMapping].
 *
 *
 * Extra method argument resolvers can be added to customize the method
 * signature that can be handled.
 *
 *
 * By default, the validation process redirects to a no-op implementation, see
 * [.setValidator] to customize it. The [ConversionService]
 * can be customized in a similar manner to tune how the message payload
 * can be converted
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @since 4.1
 * @see .setConversionService
 *
 * @see .setValidator
 *
 * @see .setCustomArgumentResolvers
 */
@Deprecated("")
class DefaultMessageHandlerMethodFactory : MessageHandlerMethodFactory, BeanFactoryAware, InitializingBean {
    private var conversionService: ConversionService = DefaultFormattingConversionService()

    @Nullable
    private var messageConverter: MessageConverter? = null

    @Nullable
    private var validator: Validator? = null

    @Nullable
    private var customArgumentResolvers: List<HandlerMethodArgumentResolver>? = null
    private val argumentResolvers = HandlerMethodArgumentResolverComposite()

    @Nullable
    private var beanFactory: BeanFactory? = null

    /**
     * Set the [ConversionService] to use to convert the original
     * message payload or headers.
     * @see org.springframework.messaging.handler.annotation.support.HeaderMethodArgumentResolver
     *
     * @see GenericMessageConverter
     */
    fun setConversionService(conversionService: ConversionService) {
        this.conversionService = conversionService
    }

    /**
     * Set the [MessageConverter] to use. By default a [GenericMessageConverter]
     * is used.
     * @see GenericMessageConverter
     */
    fun setMessageConverter(messageConverter: MessageConverter?) {
        this.messageConverter = messageConverter
    }

    /**
     * Set the Validator instance used for validating `@Payload` arguments.
     * @see org.springframework.validation.annotation.Validated
     *
     * @see PayloadMethodArgumentResolver
     */
    fun setValidator(validator: Validator?) {
        this.validator = validator
    }

    /**
     * Set the list of custom `HandlerMethodArgumentResolver`s that will be used
     * after resolvers for supported argument type.
     * @param customArgumentResolvers the list of resolvers (never `null`)
     */
    fun setCustomArgumentResolvers(customArgumentResolvers: List<HandlerMethodArgumentResolver>?) {
        this.customArgumentResolvers = customArgumentResolvers
    }

    /**
     * Configure the complete list of supported argument types effectively overriding
     * the ones configured by default. This is an advanced option. For most use cases
     * it should be sufficient to use [.setCustomArgumentResolvers].
     */
    fun setArgumentResolvers(argumentResolvers: List<HandlerMethodArgumentResolver?>?) {
        if (argumentResolvers == null) {
            this.argumentResolvers.clear()
            return
        }
        this.argumentResolvers.addResolvers(argumentResolvers)
    }

    /**
     * A [BeanFactory] only needs to be available for placeholder resolution
     * in handler method arguments; it's optional otherwise.
     */
    override fun setBeanFactory(beanFactory: BeanFactory) {
        this.beanFactory = beanFactory
    }

    override fun afterPropertiesSet() {
        if (messageConverter == null) {
            messageConverter = GenericMessageConverter(conversionService)
        }
        if (argumentResolvers.resolvers.isEmpty()) {
            argumentResolvers.addResolvers(initArgumentResolvers())
        }
    }

    override fun createInvocableHandlerMethod(bean: Any, method: Method): InvocableHandlerMethod {
        val handlerMethod = InvocableHandlerMethod(bean, method)
        handlerMethod.setMessageMethodArgumentResolvers(argumentResolvers)
        return handlerMethod
    }

    protected fun initArgumentResolvers(): List<HandlerMethodArgumentResolver> {
        val resolvers: MutableList<HandlerMethodArgumentResolver> = ArrayList()
        val beanFactory = if (beanFactory is ConfigurableBeanFactory) beanFactory as ConfigurableBeanFactory? else null

        // Annotation-based argument resolution
        resolvers.add(HeaderMethodArgumentResolver(conversionService, beanFactory))
        resolvers.add(HeadersMethodArgumentResolver())

        // Type-based argument resolution
        resolvers.add(MessageMethodArgumentResolver(messageConverter))
        if (customArgumentResolvers != null) {
            resolvers.addAll(customArgumentResolvers!!)
        }
        Assert.notNull(messageConverter, "MessageConverter not configured")
        resolvers.add(PayloadMethodArgumentResolver(messageConverter, validator))
        return resolvers
    }
}
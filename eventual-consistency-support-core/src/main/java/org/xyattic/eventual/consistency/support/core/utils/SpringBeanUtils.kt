package org.xyattic.eventual.consistency.support.core.utils

import org.apache.commons.lang3.StringUtils
import org.springframework.beans.BeanUtils
import org.springframework.beans.BeansException
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.config.AutowireCapableBeanFactory
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.support.BeanDefinitionBuilder
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.BeanNameGenerator
import org.springframework.beans.factory.support.DefaultListableBeanFactory
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.ResolvableType
import org.springframework.util.ClassUtils
import org.xyattic.eventual.consistency.support.core.exception.MqException
import java.beans.Introspector

/**
 * @author wangxing
 * @create 2020/4/8
 */
object SpringBeanUtils {
    fun <T> getBean(beanName: String): T {
        return applicationContext!!.getBean(beanName) as T
    }

    fun registerBean(beanClass: Class<*>) {
        registerBean(null, beanClass)
    }

    fun registerBean(beanClass: Class<*>?, scope: String?) {
        registerBean(null, beanClass, scope)
    }

    @JvmOverloads
    fun registerBean(beanName: String?, beanClass: Class<*>?, scope: String? = BeanDefinition.SCOPE_SINGLETON) {
        var beanName = beanName
        val beanDefinition: BeanDefinition = BeanDefinitionBuilder
                .genericBeanDefinition(beanClass)
                .setScope(scope)
                .beanDefinition
        val beanFactory: ConfigurableListableBeanFactory = beanFactory
        val registry = beanFactory as BeanDefinitionRegistry
        val beanNameGenerator = getBeanIfAvailable(BeanNameGenerator::class.java)
        if (StringUtils.isBlank(beanName)) {
            beanName = beanNameGenerator?.generateBeanName(beanDefinition, registry)
        }
        if (!registry.containsBeanDefinition(beanName)) {
            registry.registerBeanDefinition(beanName, beanDefinition)
        }
    }

    private fun getBeanDefaultName(beanClass: Class<*>): String {
        val shortClassName = ClassUtils.getShortName(beanClass)
        return Introspector.decapitalize(shortClassName)
    }

    fun <T> getBean(clz: Class<T>): T {
        return applicationContext!!.getBean(clz)
    }

    private val applicationContext: ConfigurableApplicationContext?
        get() = SpringContextUtils.applicationContext

    fun <T> getBeanIfAvailable(clz: Class<T>): T? {
        return getBeanProvider(clz).ifAvailable
    }

    val autowireCapableBeanFactory: AutowireCapableBeanFactory
        get() = applicationContext!!.autowireCapableBeanFactory
    val beanFactory: DefaultListableBeanFactory
        get() = applicationContext!!.beanFactory as DefaultListableBeanFactory

    fun autowireBean(existingBean: Any?) {
        autowireCapableBeanFactory.autowireBean(existingBean)
    }

    fun <T> autowireBean(beanClass: Class<T>): T {
        val bean: T
        bean = try {
            getBean(beanClass)
        } catch (ne: BeansException) {
            BeanUtils.instantiateClass(beanClass)
        }
        autowireBean(bean)
        return bean
    }

    fun <T> getBeanProvider(requiredType: Class<T>): ObjectProvider<T> {
        return applicationContext!!.getBeanProvider(requiredType)
    }

    fun <T> getBeanProvider(requiredType: ResolvableType): ObjectProvider<T> {
        return applicationContext!!.getBeanProvider(requiredType)
    }

    fun <T> getBean(beanName: String?, beanClass: Class<T>): T {
        val bean: T?
        if (StringUtils.isNotBlank(beanName)) {
            bean = getBean(beanName, beanClass)
        } else {
            bean = beanFactory.getBeanProvider(beanClass).ifUnique
            if (bean == null) {
                val beanNamesForType = beanFactory.getBeanNamesForType(beanClass)
                bean = if (beanNamesForType.size == 0) {
                    throw MqException(beanClass.simpleName + " bean not found in " +
                            "spring")
                } else if (beanNamesForType.size == 1) {
                    getBean(beanNamesForType[0])
                } else {
                    throw MqException(beanClass.simpleName + " bean found " + beanNamesForType.size + ", please specify a name")
                }
            }
        }
        return bean ?: throw MqException(beanClass.simpleName + " bean not found in " +
                "spring")
    }
}
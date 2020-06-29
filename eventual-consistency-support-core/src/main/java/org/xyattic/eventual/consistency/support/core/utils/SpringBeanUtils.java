package org.xyattic.eventual.consistency.support.core.utils;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.util.ClassUtils;

import java.beans.Introspector;

/**
 * @author wangxing
 * @create 2020/4/8
 */
public class SpringBeanUtils {

    @SuppressWarnings("unchecked")
    public static <T> T getBean(final String beanName) {
        return (T) getApplicationContext().getBean(beanName);
    }

    public static void registerBean(final Class beanClass) {
        registerBean(null, beanClass);
    }

    public static void registerBean(final Class beanClass, final String scope) {
        registerBean(null, beanClass, scope);
    }

    public static void registerBean(final String beanName, final Class beanClass) {
        registerBean(beanName, beanClass, BeanDefinition.SCOPE_SINGLETON);
    }

    public static void registerBean(String beanName, final Class beanClass, final String scope) {
        final BeanDefinition beanDefinition = BeanDefinitionBuilder
                .genericBeanDefinition(beanClass)
                .setScope(scope)
                .getBeanDefinition();
        final ConfigurableListableBeanFactory beanFactory = getBeanFactory();
        final BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
        final BeanNameGenerator beanNameGenerator = getBeanIfAvailable(BeanNameGenerator.class);
        if (StringUtils.isBlank(beanName)) {
            beanName = beanNameGenerator == null ? getBeanDefaultName(beanClass) :
                    beanNameGenerator.generateBeanName(beanDefinition, registry);
        }
        if (!registry.containsBeanDefinition(beanName)) {
            registry.registerBeanDefinition(beanName, beanDefinition);
        }
    }

    private static String getBeanDefaultName(final Class beanClass) {
        final String shortClassName = ClassUtils.getShortName(beanClass);
        return Introspector.decapitalize(shortClassName);
    }

    public static <T> T getBean(final Class<T> clz) {
        return getApplicationContext().getBean(clz);
    }

    private static ConfigurableApplicationContext getApplicationContext() {
        return SpringContextUtils.getApplicationContext();
    }

    public static <T> T getBeanIfAvailable(final String beanName) {
        if (getApplicationContext().containsBean(beanName)) {
            return getBean(beanName);
        }
        return null;
    }

    public static <T> T getBeanIfAvailable(final Class<T> clz) {
        return getBeanProvider(clz).getIfAvailable();
    }

    public static AutowireCapableBeanFactory getAutowireCapableBeanFactory() {
        return getApplicationContext().getAutowireCapableBeanFactory();
    }

    public static DefaultListableBeanFactory getBeanFactory() {
        return (DefaultListableBeanFactory) getApplicationContext().getBeanFactory();
    }

    public static void autowireBean(final Object existingBean) {
        getAutowireCapableBeanFactory().autowireBean(existingBean);
    }

    public static <T> T autowireBean(final Class<T> beanClass) {
        T bean;
        try {
            bean = getBean(beanClass);
        } catch (final BeansException ne) {
            bean = BeanUtils.instantiateClass(beanClass);
        }
        autowireBean(bean);
        return bean;
    }

    public static <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType) {
        return getApplicationContext().getBeanProvider(requiredType);
    }

    public static <T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType) {
        return getApplicationContext().getBeanProvider(requiredType);
    }

}
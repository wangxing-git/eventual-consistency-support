package org.xyattic.eventual.consistency.support.core.utils

import org.springframework.core.env.MutablePropertySources
import org.springframework.core.env.PropertyResolver
import org.springframework.core.env.PropertySourcesPropertyResolver
import org.springframework.lang.Nullable

object PropertyPlaceholder {

    private val log = getLogger()

    @Nullable
    fun getProperty(key: String): String? {
        return propertyResolver.getProperty(key)
    }

    fun containsProperty(key: String): Boolean {
        return propertyResolver.containsProperty(key)
    }

    fun getProperty(key: String, defaultValue: String): String {
        return propertyResolver.getProperty(key, defaultValue)
    }

    @Nullable
    fun <T> getProperty(key: String, targetType: Class<T>): T? {
        return propertyResolver.getProperty(key, targetType)
    }

    fun <T> getProperty(key: String, targetType: Class<T>, defaultValue: T): T {
        return propertyResolver.getProperty(key, targetType, defaultValue)
    }

    fun getRequiredProperty(key: String): String {
        return propertyResolver.getRequiredProperty(key)
    }

    fun <T> getRequiredProperty(key: String, targetType: Class<T>): T {
        return propertyResolver.getRequiredProperty(key, targetType)
    }

    fun resolvePlaceholders(text: String): String {
        return propertyResolver.resolvePlaceholders(text)
    }

    fun resolveRequiredPlaceholders(text: String): String {
        return propertyResolver.resolveRequiredPlaceholders(text)
    }

    private val propertyResolver: PropertyResolver
        get() = try {
            SpringContextUtils.environment
        } catch (e: Exception) {
            log.warn("无法获取spring环境变量")
            PropertySourcesPropertyResolver(MutablePropertySources())
        }
}
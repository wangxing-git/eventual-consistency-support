package org.xyattic.eventual.consistency.support.core.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.lang.Nullable;

@Slf4j
public class PropertyPlaceholder {

    @Nullable
    public static String getProperty(final String key) {
        return getPropertyResolver().getProperty(key);
    }

    public static boolean containsProperty(final String key) {
        return getPropertyResolver().containsProperty(key);
    }

    public static String getProperty(final String key, final String defaultValue) {
        return getPropertyResolver().getProperty(key, defaultValue);
    }

    @Nullable
    public static <T> T getProperty(final String key, final Class<T> targetType) {
        return getPropertyResolver().getProperty(key, targetType);
    }

    public static <T> T getProperty(final String key, final Class<T> targetType, final T defaultValue) {
        return getPropertyResolver().getProperty(key, targetType, defaultValue);
    }

    public static String getRequiredProperty(final String key) throws IllegalStateException {
        return getPropertyResolver().getRequiredProperty(key);
    }

    public static <T> T getRequiredProperty(final String key, final Class<T> targetType) throws IllegalStateException {
        return getPropertyResolver().getRequiredProperty(key, targetType);
    }

    public static String resolvePlaceholders(final String text) {
        return getPropertyResolver().resolvePlaceholders(text);
    }

    public static String resolveRequiredPlaceholders(final String text) throws IllegalArgumentException {
        return getPropertyResolver().resolveRequiredPlaceholders(text);
    }

    private static PropertyResolver getPropertyResolver() {
        try {
            return SpringContextUtils.getEnvironment();
        } catch (Exception e) {
            log.warn("无法获取spring环境变量");
            return new PropertySourcesPropertyResolver(new MutablePropertySources());
        }
    }

}
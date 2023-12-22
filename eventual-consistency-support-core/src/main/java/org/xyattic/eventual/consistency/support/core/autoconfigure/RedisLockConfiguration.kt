package org.xyattic.eventual.consistency.support.core.autoconfigure

import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.StringRedisTemplate
import org.xyattic.eventual.consistency.support.core.lock.RedisLockAspect

/**
 * @author wangxing
 * @create 2020/4/10
 */
@Configuration
@ConditionalOnClass(RedisTemplate::class)
class RedisLockConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun eventualConsistencyRedisLockAspect(redisTemplates: ObjectProvider<StringRedisTemplate>): RedisLockAspect? {
        val redisTemplate = redisTemplates.ifUnique ?: return null
        return RedisLockAspect(redisTemplate)
    }

}
package org.xyattic.eventual.consistency.support.core.lock

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.StringRedisTemplate
import java.time.Duration

/**
 * @author wangxing
 * @create 2020/4/10
 */
@Aspect
open class RedisLockAspect(private val redisTemplate: StringRedisTemplate) {
    @Value("\${eventual-consistency.redis-lock.namespace:#{'eventual-consistency:\${spring.application.name:unknown-service-name}:'}}")
    private val namespace: String? = null

    @Pointcut("@within(redisLock) || @annotation(redisLock)")
    open fun joinPoint(redisLock: RedisLock) {
    }

    @Around(value = "joinPoint(redisLock)", argNames = "pjp,redisLock")
    fun doAround(pjp: ProceedingJoinPoint, redisLock: RedisLock): Any? {
        val success = redisTemplate.opsForValue().setIfAbsent(getKey(pjp),
                Thread.currentThread().name, Duration.ofMinutes(1)) ?: false
        return if (success) {
            try {
                pjp.proceed()
            } finally {
                redisTemplate.delete(getKey(pjp))
            }
        } else null
    }

    protected fun getKey(pjp: ProceedingJoinPoint): String {
        return namespace + pjp.toString()
    }

}

package org.xyattic.eventual.consistency.support.core.lock;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Optional;

/**
 * @author wangxing
 * @create 2020/4/10
 */
@Aspect
@Slf4j
public class RedisLockAspect {

    private StringRedisTemplate redisTemplate;
    @Value("${common-mq.redis-lock.namespace:#{'common-mq:${spring.application.name:unknown-service-name}:'}}")
    private String namespace;

    public RedisLockAspect(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Pointcut("@within(redisLock) || @annotation(redisLock)")
    public void joinPoint(RedisLock redisLock) {
    }

    @Around(value = "joinPoint(redisLock)", argNames = "pjp,redisLock")
    public final Object doAround(final ProceedingJoinPoint pjp, RedisLock redisLock) throws Throwable {
        Boolean success =
                Optional.ofNullable(redisTemplate.opsForValue().setIfAbsent(getKey(pjp),
                        Thread.currentThread().getName(), Duration.ofMinutes(1))).orElse(false);
        if (success) {
            try {
                return pjp.proceed();
            } finally {
                redisTemplate.delete(getKey(pjp));
            }
        }
        return null;
    }

    protected String getKey(ProceedingJoinPoint pjp) {
        return namespace + pjp.toString();
    }

}
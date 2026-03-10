package com.finflow.aspect;

import com.finflow.annotation.RateLimited;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

@Aspect
@Component
public class RateLimiterAspect {

    private final StringRedisTemplate redisTemplate;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RateLimiterAspect.class);

    public RateLimiterAspect(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Around("@annotation(rateLimited)")
    public Object rateLimit(ProceedingJoinPoint joinPoint, RateLimited rateLimited) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = attributes.getRequest();
        String clientIp = request.getRemoteAddr();
        String endpoint = request.getRequestURI();
        String redisKey = "rate_limit:" + clientIp + ":" + endpoint;

        String currentCount = redisTemplate.opsForValue().get(redisKey);

        if (currentCount != null && Integer.parseInt(currentCount) >= rateLimited.maxRequests()) {
            log.warn("Rate limit exceeded for IP: {} on endpoint: {}", clientIp, endpoint);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Rate limit exceeded. Max " + rateLimited.maxRequests() + " requests per " + rateLimited.windowSeconds() + " seconds.");
        }

        if (currentCount == null) {
            redisTemplate.opsForValue().set(redisKey, "1", Duration.ofSeconds(rateLimited.windowSeconds()));
        } else {
            redisTemplate.opsForValue().increment(redisKey);
        }

        return joinPoint.proceed();
    }
}

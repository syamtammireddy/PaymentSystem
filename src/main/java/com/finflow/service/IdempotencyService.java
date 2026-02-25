package com.finflow.service;




import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service

public class IdempotencyService {


    private final StringRedisTemplate redisTemplate;

    public IdempotencyService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    private static final String IDEMPOTENCY_PREFIX = "idemp:";
    private static final Duration EXPIRY = Duration.ofHours(24);

    /**
     * Checks if the key exists. If so, returns the cached transaction ref.
     * If not, it reserves the key (setIfAbsent) and returns null.
     */
    public String getOrReserve(String idempotencyKey, String newTransactionRef) {
        String redisKey = IDEMPOTENCY_PREFIX + idempotencyKey;
        
        Boolean isNew = redisTemplate.opsForValue().setIfAbsent(redisKey, newTransactionRef, EXPIRY);
        
        if (Boolean.TRUE.equals(isNew)) {
            return null; // It was new, go ahead and process
        }
        
        return redisTemplate.opsForValue().get(redisKey);
    }
}

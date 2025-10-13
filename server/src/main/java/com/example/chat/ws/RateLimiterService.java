package com.example.chat.ws;

import com.google.common.util.concurrent.RateLimiter; // Cần thêm dependency nếu chưa có
import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class RateLimiterService {
    // Guava RateLimiter is a simple option. For production, consider bucket4j or Resilience4j.
    // Let's use a simple manual token bucket to avoid adding a new dependency.
    private static class TokenBucket {
        private final long capacity;
        private final double tokensPerSecond;
        private long lastRefillTimestamp;
        private double availableTokens;

        public TokenBucket(long capacity, double tokensPerSecond) {
            this.capacity = capacity;
            this.tokensPerSecond = tokensPerSecond;
            this.lastRefillTimestamp = System.nanoTime();
            this.availableTokens = capacity;
        }

        public synchronized boolean tryConsume() {
            refill();
            if (availableTokens >= 1) {
                availableTokens--;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.nanoTime();
            double elapsedSeconds = (now - lastRefillTimestamp) / 1_000_000_000.0;
            double newTokens = elapsedSeconds * tokensPerSecond;
            availableTokens = Math.min(capacity, availableTokens + newTokens);
            lastRefillTimestamp = now;
        }
    }

    private final ConcurrentHashMap<String, TokenBucket> limiters = new ConcurrentHashMap<>();

    public boolean tryConsume(String key) {
        // 5 messages per second with a burst of 10
        TokenBucket limiter = limiters.computeIfAbsent(key, k -> new TokenBucket(10, 5.0));
        return limiter.tryConsume();
    }

    public void removeLimiter(String key) {
        limiters.remove(key);
    }
}
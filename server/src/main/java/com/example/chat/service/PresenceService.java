package com.example.chat.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.Set;

@Service
public class PresenceService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String PRESENCE_KEY_PREFIX = "presence:";

    public PresenceService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void userOnline(String username, String room) {
        redisTemplate.opsForSet().add(PRESENCE_KEY_PREFIX + room, username);
    }

    public void userOffline(String username, String room) {
        redisTemplate.opsForSet().remove(PRESENCE_KEY_PREFIX + room, username);
    }

    public Set<String> getOnlineUsers(String room) {
        return redisTemplate.opsForSet().members(PRESENCE_KEY_PREFIX + room);
    }
}
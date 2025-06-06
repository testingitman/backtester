package com.backtester;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

public class RedisStore {
    private static final Logger logger = LoggerFactory.getLogger(RedisStore.class);
    private static final Jedis jedis = new Jedis("redis://localhost:6379");

    public static String get(String key) {
        try {
            String val = jedis.get(key);
            logger.debug("Redis GET {} -> {}", key, val);
            return val;
        } catch (JedisConnectionException e) {
            logger.error("Redis not available", e);
            return null;
        }
    }

    public static void set(String key, String value) {
        try {
            jedis.set(key, value);
            logger.debug("Redis SET {}", key);
        } catch (JedisConnectionException e) {
            logger.error("Redis not available", e);
        }
    }
}

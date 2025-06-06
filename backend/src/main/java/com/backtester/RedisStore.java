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
            return jedis.get(key);
        } catch (JedisConnectionException e) {
            logger.error("Redis not available", e);
            return null;
        }
    }

    public static void set(String key, String value) {
        try {
            jedis.set(key, value);
        } catch (JedisConnectionException e) {
            logger.error("Redis not available", e);
        }
    }
}

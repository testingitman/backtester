package com.backtester.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import redis.clients.jedis.Jedis;

import java.util.*;

@RestController
@RequestMapping("/api/feed")
public class FeedController {
    private final Jedis jedis = new Jedis("localhost");
    private final ObjectMapper mapper = new ObjectMapper();

    @GetMapping
    public List<Map<String, Object>> list() {
        Set<String> keys = jedis.keys("headline:*");
        List<Map<String, Object>> out = new ArrayList<>();
        for (String k : keys) {
            try {
                Map<String, Object> val = mapper.readValue(jedis.get(k), Map.class);
                val.put("id", k.substring("headline:".length()));
                double close = 0.0;
                if (val.containsKey("close")) {
                    close = ((Number) val.get("close")).doubleValue();
                }
                double current = close + (Math.random() - 0.5) * close * 0.02;
                val.put("current", current);
                String action = "";
                if (val.containsKey("analysis")) {
                    Object a = ((Map<?, ?>) val.get("analysis")).get("action");
                    if (a != null) action = a.toString();
                }
                double pct = close != 0 ? (current - close) / close * 100.0 : 0.0;
                if ("sell".equalsIgnoreCase(action)) pct *= -1;
                val.put("changePct", pct);
                out.add(val);
            } catch (Exception ignored) {
            }
        }
        out.sort((a, b) -> Double.compare(
                ((Number) b.getOrDefault("timestamp", 0)).doubleValue(),
                ((Number) a.getOrDefault("timestamp", 0)).doubleValue()));
        return out;
    }
}


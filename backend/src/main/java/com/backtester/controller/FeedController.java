package com.backtester.controller;

import com.backtester.Config;
import com.backtester.service.RssService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import redis.clients.jedis.Jedis;

import java.util.*;

@RestController
@RequestMapping("/api/feed")
public class FeedController {
    // Jedis(String) expects a redis URI; provide host and port explicitly
    private final Jedis jedis = new Jedis("redis://localhost:6379");
    private final ObjectMapper mapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(FeedController.class);

    @Autowired
    private RssService rssService;

    @GetMapping
    public ResponseEntity<?> list() {
        logger.debug("Fetching RSS feed entries from redis");
        try {
            Set<String> keys = jedis.keys("headline:*");
            logger.debug("Found {} entries in redis", keys.size());
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
                } catch (Exception e) {
                    logger.warn("Failed to parse entry {}", k, e);
                }
            }
            out.sort((a, b) -> Double.compare(
                    ((Number) b.getOrDefault("timestamp", 0)).doubleValue(),
                    ((Number) a.getOrDefault("timestamp", 0)).doubleValue()));
            return ResponseEntity.ok(out);
        } catch (Exception e) {
            logger.error("Failed to fetch feed from redis", e);
            Map<String, String> err = new HashMap<>();
            err.put("error", "Redis not available");
            err.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(err);
        }
    }

    @GetMapping("/remote")
    public ResponseEntity<?> remote() {
        String url = Config.get("rss_feed_url");
        var feed = rssService.fetchFeed(url);
        if (feed == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch feed"));
        }
        List<Map<String, Object>> out = new ArrayList<>();
        feed.getEntries().forEach(e -> {
            Map<String, Object> m = new HashMap<>();
            m.put("title", e.getTitle());
            m.put("link", e.getLink());
            if (e.getPublishedDate() != null) {
                m.put("published", e.getPublishedDate().getTime());
            }
            out.add(m);
        });
        return ResponseEntity.ok(out);
    }
}


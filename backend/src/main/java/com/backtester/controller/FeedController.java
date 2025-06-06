package com.backtester.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.backtester.service.FeedService;
import com.backtester.service.QuoteService;
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
    private FeedService feedService;

    @Autowired
    private QuoteService quoteService;

    @GetMapping
    public ResponseEntity<?> list() {
        try {
            feedService.refreshFeeds();
        } catch (Exception e) {
            logger.warn("Failed to refresh feed", e);
        }
        logger.debug("Fetching RSS feed entries from redis");
        try {
            Set<String> keys = jedis.keys("headline:*");
            logger.debug("Found {} entries in redis", keys.size());
            List<Map<String, Object>> out = new ArrayList<>();
            for (String k : keys) {
                try {
                    Map<String, Object> val = mapper.readValue(jedis.get(k), Map.class);
                    val.put("id", k.substring("headline:".length()));
                    boolean analysed = Boolean.TRUE.equals(val.get("analysed"));
                    double close = 0.0;
                    if (val.containsKey("close")) {
                        close = ((Number) val.get("close")).doubleValue();
                    }
                    double current = close + (Math.random() - 0.5) * close * 0.02;
                    val.put("current", current);
                    String action = "";
                    double confidence = 0.0;
                    List<Map<String, Object>> stocks = new ArrayList<>();
                    if (analysed && val.containsKey("analysis")) {
                        Map<?, ?> analysis = (Map<?, ?>) val.get("analysis");
                        Object a = analysis.get("action");
                        if (a != null) action = a.toString();
                        Object c = analysis.get("confidence");
                        if (c instanceof Number) confidence = ((Number) c).doubleValue();
                        Object tokensObj = analysis.get("tokens");
                        List<String> tokens = new ArrayList<>();
                        if (tokensObj instanceof List) {
                            for (Object t : (List<?>) tokensObj) {
                                if (t != null) tokens.add(t.toString());
                            }
                        } else if (tokensObj instanceof String) {
                            for (String p : ((String) tokensObj).split(",")) {
                                String s = p.trim();
                                if (!s.isEmpty()) tokens.add(s);
                            }
                        }
                        for (String sym : tokens) {
                            Map<String, Object> sm = new HashMap<>();
                            sm.put("symbol", sym);
                            sm.put("token", quoteService.resolveToken(sym));
                            sm.put("close", close);
                            sm.put("current", current);
                            sm.put("action", action);
                            sm.put("confidence", confidence);
                            stocks.add(sm);
                        }
                    }
                    double pct = close != 0 ? (current - close) / close * 100.0 : 0.0;
                    if ("sell".equalsIgnoreCase(action)) pct *= -1;
                    val.put("changePct", pct);
                    val.put("stocks", stocks);
                    val.put("analysed", analysed);
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
        Map<String, Object> res = feedService.refreshFeeds();
        return ResponseEntity.ok(res);
    }

    @DeleteMapping
    public ResponseEntity<?> clear() {
        try {
            Set<String> keys = jedis.keys("headline:*");
            if (!keys.isEmpty()) {
                jedis.del(keys.toArray(new String[0]));
            }
            Map<String, Object> res = new HashMap<>();
            res.put("removed", keys.size());
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            logger.error("Failed to clear feed storage", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Failed"));
        }
    }
}


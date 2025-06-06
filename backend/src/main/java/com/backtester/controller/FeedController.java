package com.backtester.controller;

import com.backtester.Config;
import com.backtester.service.RssService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.feed.synd.SyndEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import redis.clients.jedis.Jedis;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@RestController
@RequestMapping("/api/feed")
public class FeedController {
    // Jedis(String) expects a redis URI; provide host and port explicitly
    private final Jedis jedis = new Jedis("redis://localhost:6379");
    private final ObjectMapper mapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(FeedController.class);
    private static final String PROMPT = "Analyze this news item and respond with JSON. " +
            "Fields: tokens (list of affected NSE symbols), action (Buy or Sell), " +
            "confidence (0-10), term ('short' or 'long' for expected profit horizon), " +
            "reason. Use concise JSON only. News: '%s - %s'.";

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

    private Map<String, Object> analyze(String title, String description) {
        String prompt = String.format(PROMPT, title, description == null ? "" : description);
        try {
            URL url = new URL("https://api.groq.com/openai/v1/chat/completions");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + Config.get("groq_api_key"));
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setDoOutput(true);
            String payload = mapper.writeValueAsString(Map.of(
                    "model", "gpt-4",
                    "messages", List.of(Map.of("role", "user", "content", prompt))
            ));
            conn.getOutputStream().write(payload.getBytes(StandardCharsets.UTF_8));
            JsonNode root = mapper.readTree(conn.getInputStream());
            String text = root.path("choices").get(0).path("message").path("content").asText().trim();
            Map<String, Object> data;
            try {
                data = mapper.readValue(text, Map.class);
                Object tokens = data.get("tokens");
                if (tokens instanceof String) {
                    String[] parts = ((String) tokens).split(",");
                    List<String> list = new ArrayList<>();
                    for (String p : parts) {
                        String t = p.trim().replaceAll("^['\"]|['\"]$", "");
                        if (!t.isEmpty()) list.add(t);
                    }
                    data.put("tokens", list);
                }
            } catch (Exception e) {
                data = new HashMap<>();
                data.put("error", "Failed to parse");
                data.put("raw", text);
            }
            return data;
        } catch (Exception e) {
            logger.error("Failed to analyze feed", e);
            Map<String, Object> err = new HashMap<>();
            err.put("error", "Request failed");
            err.put("message", e.getMessage());
            return err;
        }
    }

    @GetMapping("/remote")
    public ResponseEntity<?> remote() {
        String url = Config.get("rss_feed_url");
        SyndFeed feed = rssService.fetchFeed(url);
        if (feed == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch feed"));
        }
        int processed = 0;
        for (SyndEntry entry : feed.getEntries()) {
            try {
                String id = sha1(entry.getLink());
                String key = "headline:" + id;
                if (jedis.exists(key)) {
                    continue;
                }
                Map<String, Object> stored = new HashMap<>();
                stored.put("title", entry.getTitle());
                stored.put("link", entry.getLink());
                Map<String, Object> analysis = analyze(entry.getTitle(),
                        entry.getDescription() != null ? entry.getDescription().getValue() : "");
                stored.put("analysis", analysis);
                stored.put("timestamp", System.currentTimeMillis() / 1000);
                stored.put("close", Math.round((90 + Math.random() * 20) * 100.0) / 100.0);
                jedis.set(key, mapper.writeValueAsString(stored));
                processed++;
            } catch (Exception e) {
                logger.warn("Failed to process entry {}", entry.getLink(), e);
            }
        }
        return ResponseEntity.ok(Map.of("processed", processed));
    }

    private String sha1(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}


package com.backtester.service;

import com.backtester.Config;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import java.io.FileWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

@Service
public class FeedService {
    private static final Logger logger = LoggerFactory.getLogger(FeedService.class);
    private static final String LAST_FETCH_KEY = "rss:last_fetch";
    private static final long MIN_INTERVAL = 300; // 5 minutes
    private final Jedis jedis = new Jedis("redis://localhost:6379");
    private final ObjectMapper mapper = new ObjectMapper();

    private static final List<String> RSS_FEEDS = Arrays.asList(
            "https://www.business-standard.com/rss/markets-106.rss",
            "https://www.business-standard.com/rss/companies-102.rss",
            "https://www.business-standard.com/rss/economy-101.rss",
            "https://www.livemint.com/rss/markets",
            "https://www.moneycontrol.com/rss/markets.xml",
            "https://www.moneycontrol.com/rss/MCtopnews.xml",
            "https://www.moneycontrol.com/rss/mfnews.xml",
            "https://www.moneycontrol.com/rss/iponews.xml",
            "https://economictimes.indiatimes.com/rssfeedsdefault.cms",
            "https://economictimes.indiatimes.com/markets/rssfeeds/1977021501.cms",
            "https://economictimes.indiatimes.com/markets/sensex/rssfeeds/2146841734.cms",
            "https://economictimes.indiatimes.com/markets/ipos/rssfeeds/70323525.cms"
    );

    @Autowired
    private RssService rssService;

    public synchronized Map<String, Object> refreshFeeds() {
        long now = System.currentTimeMillis() / 1000;
        try {
            String ts = jedis.get(LAST_FETCH_KEY);
            if (ts != null && now - Long.parseLong(ts) < MIN_INTERVAL) {
                logger.debug("RSS fetch skipped; last fetch {} seconds ago", now - Long.parseLong(ts));
                Map<String, Object> res = new HashMap<>();
                res.put("skipped", true);
                return res;
            }
        } catch (Exception e) {
            logger.warn("Failed to check last fetch", e);
        }
        int processed = 0;
        for (String url : RSS_FEEDS) {
            SyndFeed feed = rssService.fetchFeed(url);
            if (feed == null) continue;
            for (SyndEntry entry : feed.getEntries()) {
                try {
                    String id = sha1(entry.getLink());
                    String key = "headline:" + id;
                    Map<String, Object> stored;
                    if (jedis.exists(key)) {
                        stored = mapper.readValue(jedis.get(key), Map.class);
                        if (Boolean.TRUE.equals(stored.get("analysed"))) {
                            continue;
                        }
                    } else {
                        stored = new HashMap<>();
                        stored.put("title", entry.getTitle());
                        stored.put("link", entry.getLink());
                        Date pub = entry.getPublishedDate();
                        if (pub == null) pub = entry.getUpdatedDate();
                        long ts = pub != null ? pub.getTime() / 1000 : now;
                        stored.put("timestamp", ts);
                        stored.put("close", Math.round((90 + Math.random() * 20) * 100.0) / 100.0);
                    }
                    Map<String, Object> analysis = analyze(
                            entry.getTitle(),
                            entry.getDescription() != null ? entry.getDescription().getValue() : "",
                            entry.getLink());
                    if (analysis != null) {
                        stored.put("analysis", analysis);
                        stored.put("analysed", true);
                    } else {
                        stored.remove("analysis");
                        stored.put("analysed", false);
                    }
                    jedis.set(key, mapper.writeValueAsString(stored));
                    try (FileWriter fw = new FileWriter("feed.jsonl", true)) {
                        fw.write(mapper.writeValueAsString(stored));
                        fw.write("\n");
                    } catch (Exception e) {
                        logger.warn("Failed to persist feed entry", e);
                    }
                    processed++;
                } catch (Exception e) {
                    logger.warn("Failed to process entry {}", entry.getLink(), e);
                }
            }
        }
        jedis.set(LAST_FETCH_KEY, String.valueOf(now));
        return Collections.singletonMap("processed", processed);
    }

    @Scheduled(fixedDelay = MIN_INTERVAL * 1000)
    public void scheduledFetch() {
        try {
            refreshFeeds();
        } catch (Exception e) {
            logger.warn("Scheduled fetch failed", e);
        }
    }

    private Map<String, Object> analyze(String title, String description, String link) throws Exception {
        String content = fetchArticle(link);
        if (content.length() > 1000) {
            content = content.substring(0, 1000);
        }
        String prompt = String.format(
                "Analyze this news item and respond with JSON. Fields: tokens (list of affected NSE scrip names), action (Buy or Sell), confidence (0-10), term ('short' or 'long' for expected profit horizon), reason. Use concise JSON only. News: '%s - %s - %s'.",
                title,
                description == null ? "" : description,
                content);

        URL url = new URL("https://api.groq.com/openai/v1/chat/completions");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + Config.get("groq_api_key"));
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        conn.setDoOutput(true);

        ObjectNode payload = mapper.createObjectNode();
        payload.put("model", "meta-llama/llama-4-scout-17b-16e-instruct");
        ObjectNode msg = mapper.createObjectNode();
        msg.put("role", "user");
        msg.put("content", prompt);
        payload.putArray("messages").add(msg);
        payload.put("temperature", 1);
        payload.put("max_completion_tokens", 1024);
        payload.put("top_p", 1);
        payload.put("stream", false);
        String body = mapper.writeValueAsString(payload);

        conn.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
        int code = conn.getResponseCode();
        if (code != 200) {
            logger.warn("Groq API returned {}", code);
            return null;
        }
        JsonNode root = mapper.readTree(conn.getInputStream());
        String text = root.path("choices").get(0).path("message").path("content").asText().trim();
        try {
            Map<String, Object> data = mapper.readValue(text, Map.class);
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
            return data;
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "Failed to parse");
            err.put("raw", text);
            return err;
        }
    }

    private String sha1(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private String fetchArticle(String link) {
        try {
            Document doc = Jsoup.connect(link).userAgent("Mozilla/5.0")
                    .timeout(10000).get();
            return doc.text();
        } catch (Exception e) {
            logger.warn("Failed to fetch article {}", link, e);
            return "";
        }
    }
}

package com.backtester.controller;

import com.backtester.Config;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/grok")
public class GrokController {
    private static final Logger logger = LoggerFactory.getLogger(GrokController.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    @PostMapping
    public ResponseEntity<?> query(@RequestBody Map<String, String> body) {
        String q = body.getOrDefault("q", "").trim();
        if (q.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Empty query"));
        }
        try {
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
            msg.put("content", q);
            payload.putArray("messages").add(msg);
            payload.put("temperature", 1);
            payload.put("max_completion_tokens", 1024);
            payload.put("top_p", 1);
            payload.put("stream", false);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(mapper.writeValueAsBytes(payload));
            }
            int code = conn.getResponseCode();
            if (code != 200) {
                logger.warn("Groq API returned {}", code);
                Map<String, Object> err = new HashMap<>();
                err.put("error", "Failed");
                err.put("code", code);
                return ResponseEntity.status(code).body(err);
            }
            JsonNode root = mapper.readTree(conn.getInputStream());
            String text = root.path("choices").get(0).path("message").path("content").asText();
            Map<String, Object> res = new HashMap<>();
            res.put("answer", text.trim());
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            logger.error("Grok query failed", e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed"));
        }
    }
}

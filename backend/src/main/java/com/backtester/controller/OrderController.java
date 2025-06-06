package com.backtester.controller;

import com.backtester.Config;
import com.backtester.RedisStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/order")
public class OrderController {
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    @GetMapping("/balance")
    public ResponseEntity<Map<String, Object>> balance() {
        String apiKey = Config.get("kite_api_key");
        String access = RedisStore.get("kite_access_token");
        Map<String, Object> res = new HashMap<>();
        if (access == null || access.isEmpty()) {
            res.put("error", "Not authenticated");
            return ResponseEntity.status(401).body(res);
        }
        try {
            URL url = new URL("https://api.kite.trade/margins/equity");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("X-Kite-Version", "3");
            conn.setRequestProperty("Authorization", String.format("token %s:%s", apiKey, access));
            if (conn.getResponseCode() != 200) {
                res.put("error", "Failed to fetch balance");
                return ResponseEntity.status(conn.getResponseCode()).body(res);
            }
            JsonNode root = mapper.readTree(conn.getInputStream());
            double cash = root.path("data").path("available").path("cash").asDouble();
            res.put("cash", cash);
            return ResponseEntity.ok(res);
        } catch (IOException e) {
            logger.error("Balance request failed", e);
            res.put("error", "Request failed");
            return ResponseEntity.status(500).body(res);
        }
    }

    @PostMapping("/place")
    public ResponseEntity<Map<String, Object>> place(@RequestBody Map<String, String> body) {
        String apiKey = Config.get("kite_api_key");
        String access = RedisStore.get("kite_access_token");
        Map<String, Object> res = new HashMap<>();
        if (access == null || access.isEmpty()) {
            res.put("error", "Not authenticated");
            return ResponseEntity.status(401).body(res);
        }
        String symbol = body.get("symbol");
        String action = body.getOrDefault("side", "BUY");
        String qtyStr = body.getOrDefault("quantity", "0");
        String priceStr = body.getOrDefault("price", "0");
        int quantity = Integer.parseInt(qtyStr);
        double price = Double.parseDouble(priceStr);
        try {
            URL url = new URL("https://api.kite.trade/orders/regular");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("X-Kite-Version", "3");
            conn.setRequestProperty("Authorization", String.format("token %s:%s", apiKey, access));
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setDoOutput(true);
            String payload = String.format("exchange=NSE&tradingsymbol=%s&transaction_type=%s&quantity=%d&price=%f&order_type=LIMIT&product=CNC&validity=DAY",
                    symbol, action.toUpperCase(), quantity, price);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }
            if (conn.getResponseCode() != 200) {
                res.put("error", "Order failed");
                res.put("code", conn.getResponseCode());
                return ResponseEntity.status(conn.getResponseCode()).body(res);
            }
            JsonNode root = mapper.readTree(conn.getInputStream());
            res.put("data", root.path("data"));
            return ResponseEntity.ok(res);
        } catch (IOException e) {
            logger.error("Order placement failed", e);
            res.put("error", "Request failed");
            return ResponseEntity.status(500).body(res);
        }
    }
}

package com.backtester.service;

import com.backtester.Config;
import com.backtester.RedisStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@Service
public class OrderService {
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
    private final ObjectMapper mapper = new ObjectMapper();

    private HttpURLConnection makeConn(String endpoint, String method) throws IOException {
        String apiKey = Config.get("kite_api_key");
        String access = RedisStore.get("kite_access_token");
        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("X-Kite-Version", "3");
        conn.setRequestProperty("Authorization", String.format("token %s:%s", apiKey, access));
        return conn;
    }

    public double getFunds() {
        try {
            HttpURLConnection conn = makeConn("https://api.kite.trade/user/margins", "GET");
            if (conn.getResponseCode() != 200) {
                logger.warn("Funds request failed with code {}", conn.getResponseCode());
                return 0.0;
            }
            JsonNode root = mapper.readTree(conn.getInputStream());
            return root.path("data").path("equity").path("available").path("cash").asDouble();
        } catch (Exception e) {
            logger.error("Failed to fetch funds", e);
            return 0.0;
        }
    }

    public Map<String, Object> placeOrder(String tradingsymbol, String exchange, String action,
                                          int quantity, double price) {
        try {
            HttpURLConnection conn = makeConn("https://api.kite.trade/orders/regular", "POST");
            conn.setDoOutput(true);
            String body = String.format(
                    "tradingsymbol=%s&exchange=%s&transaction_type=%s&order_type=LIMIT&quantity=%d&price=%.2f&product=MIS&validity=DAY",
                    tradingsymbol, exchange, action.toUpperCase(), quantity, price);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes());
            }
            if (conn.getResponseCode() != 200) {
                logger.warn("Order failed with code {}", conn.getResponseCode());
                return Map.of("error", "Order rejected", "code", conn.getResponseCode());
            }
            JsonNode root = mapper.readTree(conn.getInputStream());
            Map<String, Object> res = new HashMap<>();
            res.put("status", root.path("status").asText());
            res.put("order_id", root.path("data").path("order_id").asText());
            return res;
        } catch (Exception e) {
            logger.error("Failed to place order", e);
            return Map.of("error", e.getMessage());
        }
    }
}

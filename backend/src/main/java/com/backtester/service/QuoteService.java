package com.backtester.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.backtester.Config;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class QuoteService {
    // Jedis(String) expects a redis URI starting with redis://
    private final Jedis jedis = new Jedis("redis://localhost:6379");
    private final Map<String, List<Double>> memoryCache = new ConcurrentHashMap<>();

    public List<Double> getPrices(String symbol, String period, String from, String to) {
        String key = symbol + ":" + period + ":" + from + ":" + to;
        if (memoryCache.containsKey(key)) {
            return memoryCache.get(key);
        } else if (jedis.exists(key)) {
            List<Double> cached = parse(jedis.lrange(key, 0, -1));
            memoryCache.put(key, cached);
            return cached;
        }

        // Placeholder for Zerodha KITE API call
        List<Double> prices = fetchFromKite(symbol, period, from, to);

        if (prices != null) {
            for (Double p : prices) {
                jedis.rpush(key, String.valueOf(p));
            }
            memoryCache.put(key, prices);
        }
        return prices;
    }

    private List<Double> parse(List<String> values) {
        List<Double> list = new ArrayList<>();
        for (String v : values) {
            list.add(Double.parseDouble(v));
        }
        return list;
    }

    private List<Double> fetchFromKite(String symbol, String period, String from, String to) {
        String apiKey = Config.get("kite_api_key");
        String accessToken = Config.get("kite_access_token");
        String url = String.format(
                "https://api.kite.trade/instruments/historical/%s/%s?from=%s&to=%s&api_key=%s&access_token=%s",
                symbol, period, from, to, apiKey, accessToken);
        try {
            URL u = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) u.openConnection();
            conn.setRequestProperty("X-Kite-Version", "3");
            conn.setRequestMethod("GET");
            if (conn.getResponseCode() != 200) {
                return new ArrayList<>();
            }
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(conn.getInputStream());
            List<Double> list = new ArrayList<>();
            for (JsonNode candle : root.path("data").path("candles")) {
                list.add(candle.get(4).asDouble()); // close price
            }
            return list;
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}

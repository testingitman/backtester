package com.backtester.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.backtester.Config;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class QuoteService {
    private final Jedis jedis = new Jedis("localhost");
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
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("X-Kite-Version", "3")
                .build();
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return new ArrayList<>();
            }
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.body());
            List<Double> list = new ArrayList<>();
            for (JsonNode candle : root.path("data").path("candles")) {
                list.add(candle.get(4).asDouble()); // close price
            }
            return list;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}

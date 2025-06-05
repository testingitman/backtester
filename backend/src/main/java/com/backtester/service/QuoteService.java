package com.backtester.service;

import com.backtester.model.Candle;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final Map<String, List<Candle>> memoryCache = new ConcurrentHashMap<>();

    public List<Double> getPrices(String symbol, String period, String from, String to) {
        List<Candle> candles = getCandles(symbol, period, from, to);
        List<Double> prices = new ArrayList<>();
        for (Candle c : candles) prices.add(c.getClose());
        return prices;
    }

    public List<Candle> getCandles(String symbol, String period, String from, String to) {
        String key = symbol + ":" + period + ":" + from + ":" + to;
        if (memoryCache.containsKey(key)) {
            return memoryCache.get(key);
        } else if (jedis.exists(key)) {
            List<Candle> cached = parseCandles(jedis.lrange(key, 0, -1));
            memoryCache.put(key, cached);
            return cached;
        }

        List<Candle> candles = fetchFromKite(symbol, period, from, to);
        if (candles != null) {
            for (Candle c : candles) {
                String line = String.join(",",
                        c.getTime(),
                        Double.toString(c.getOpen()),
                        Double.toString(c.getHigh()),
                        Double.toString(c.getLow()),
                        Double.toString(c.getClose()),
                        Double.toString(c.getVolume()));
                jedis.rpush(key, line);
            }
            memoryCache.put(key, candles);
        }
        return candles;
    }

    private List<Candle> parseCandles(List<String> values) {
        List<Candle> list = new ArrayList<>();
        for (String v : values) {
            String[] parts = v.split(",");
            if (parts.length < 6) continue;
            Candle c = new Candle(parts[0],
                    Double.parseDouble(parts[1]),
                    Double.parseDouble(parts[2]),
                    Double.parseDouble(parts[3]),
                    Double.parseDouble(parts[4]),
                    Double.parseDouble(parts[5]));
            list.add(c);
        }
        return list;
    }

    private List<Candle> fetchFromKite(String symbol, String period, String from, String to) {
        String apiKey = System.getenv("KITE_API_KEY");
        String accessToken = System.getenv("KITE_ACCESS_TOKEN");
        String url = String.format(
                "https://api.kite.trade/instruments/historical/%s/%s?from=%s&to=%s&interval=%s&api_key=%s&access_token=%s",
                symbol, period, from, to, period, apiKey, accessToken);
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
            List<Candle> list = new ArrayList<>();
            for (JsonNode c : root.path("data").path("candles")) {
                Candle candle = new Candle(
                        c.get(0).asText(),
                        c.get(1).asDouble(),
                        c.get(2).asDouble(),
                        c.get(3).asDouble(),
                        c.get(4).asDouble(),
                        c.get(5).asDouble());
                list.add(candle);
            }
            return list;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}

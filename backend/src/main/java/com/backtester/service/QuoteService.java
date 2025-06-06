package com.backtester.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.backtester.Config;
import com.backtester.RedisStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class QuoteService {
    // Jedis(String) expects a redis URI starting with redis://
    private final Jedis jedis = new Jedis("redis://localhost:6379");
    private final Map<String, List<Double>> memoryCache = new ConcurrentHashMap<>();
    private List<Map<String, String>> instrumentCache = null;
    private static final Logger logger = LoggerFactory.getLogger(QuoteService.class);

    public List<Double> getPrices(String symbol, String period, String from, String to) {
        String token = resolveToken(symbol);
        String key = token + ":" + period + ":" + from + ":" + to;
        logger.debug("Fetching prices for {} {} from {} to {}", token, period, from, to);
        try {
            if (memoryCache.containsKey(key)) {
                logger.debug("Returning prices from memory cache for {}", key);
                return memoryCache.get(key);
            } else if (jedis.exists(key)) {
                logger.debug("Returning prices from redis cache for {}", key);
                List<Double> cached = parse(jedis.lrange(key, 0, -1));
                memoryCache.put(key, cached);
                return cached;
            }
        } catch (JedisConnectionException e) {
            logger.error("Redis not available", e);
            throw new IllegalStateException("Redis not available", e);
        }

        // Placeholder for Zerodha KITE API call
        List<Double> prices = fetchFromKite(token, period, from, to);

        if (prices != null) {
            try {
                logger.debug("Caching {} prices for {}", prices.size(), key);
                for (Double p : prices) {
                    jedis.rpush(key, String.valueOf(p));
                }
                memoryCache.put(key, prices);
            } catch (JedisConnectionException e) {
                logger.error("Redis not available", e);
                throw new IllegalStateException("Redis not available", e);
            }
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
        String accessToken = RedisStore.get("kite_access_token");
        String url = String.format(
                "https://api.kite.trade/instruments/historical/%s/%s?from=%s&to=%s&api_key=%s&access_token=%s",
                symbol, period, from, to, apiKey, accessToken);
        try {
            logger.debug("Requesting data from KITE: {}", url);
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
            logger.debug("Received {} candles for {}", list.size(), symbol);
            return list;
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public synchronized List<Map<String, String>> getInstruments() {
        if (instrumentCache != null) {
            return instrumentCache;
        }
        try {
            String json = jedis.get("instruments");
            if (json != null) {
                ObjectMapper mapper = new ObjectMapper();
                instrumentCache = mapper.readValue(json, List.class);
                return instrumentCache;
            }
        } catch (JedisConnectionException | IOException e) {
            logger.error("Redis not available", e);
        }
        List<Map<String, String>> data = fetchInstrumentDump();
        if (data != null) {
            try {
                jedis.set("instruments", new ObjectMapper().writeValueAsString(data));
            } catch (JedisConnectionException | IOException e) {
                logger.error("Redis not available", e);
            }
        }
        instrumentCache = data;
        return data;
    }

    private List<Map<String, String>> fetchInstrumentDump() {
        String apiKey = Config.get("kite_api_key");
        String accessToken = RedisStore.get("kite_access_token");
        if (accessToken == null || accessToken.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            logger.debug("Fetching instrument dump from KITE");
            URL u = new URL("https://api.kite.trade/instruments");
            HttpURLConnection conn = (HttpURLConnection) u.openConnection();
            conn.setRequestProperty("X-Kite-Version", "3");
            conn.setRequestProperty("Authorization", "token " + apiKey + ":" + accessToken);
            conn.setRequestMethod("GET");
            if (conn.getResponseCode() != 200) {
                logger.warn("Instruments request failed with code {}", conn.getResponseCode());
                return new ArrayList<>();
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line = br.readLine(); // header
            List<Map<String, String>> list = new ArrayList<>();
            while ((line = br.readLine()) != null) {
                String[] p = line.split(",", -1);
                if (p.length > 3) {
                    Map<String, String> m = new HashMap<>();
                    m.put("token", p[0]);
                    m.put("tradingsymbol", p[2]);
                    m.put("name", p[3]);
                    list.add(m);
                }
            }
            return list;
        } catch (IOException e) {
            logger.error("Failed to fetch instruments", e);
            return new ArrayList<>();
        }
    }

    private String resolveToken(String symbol) {
        if (symbol.matches("\\d+")) return symbol;
        for (Map<String, String> m : getInstruments()) {
            String ts = m.get("tradingsymbol");
            String name = m.get("name");
            if (symbol.equalsIgnoreCase(ts) || symbol.equalsIgnoreCase(name)) {
                return m.get("token");
            }
        }
        return symbol;
    }
}

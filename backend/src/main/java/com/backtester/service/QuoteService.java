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
import redis.clients.jedis.resps.Tuple;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class QuoteService {
    // Jedis(String) expects a redis URI starting with redis://
    private final Jedis jedis;

    public QuoteService() {
        this(new Jedis("redis://localhost:6379"));
    }

    public QuoteService(Jedis jedis) {
        this.jedis = jedis;
    }
    private final Map<String, NavigableMap<Long, Double>> memoryCache = new ConcurrentHashMap<>();
    private List<Map<String, String>> instrumentCache = null;
    private static final Logger logger = LoggerFactory.getLogger(QuoteService.class);
    private static final Set<String> NIFTY_SYMBOLS = loadNiftySymbols();

    static class Candle {
        final long timestamp;
        final double close;
        Candle(long timestamp, double close) { this.timestamp = timestamp; this.close = close; }
    }

    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");

    private long parseDate(String d) {
        return LocalDate.parse(d).atStartOfDay(ZoneOffset.UTC).toEpochSecond();
    }

    private long parseTimestamp(String ts) {
        return ZonedDateTime.parse(ts, TS_FORMAT).toInstant().getEpochSecond();
    }

    public List<Double> getPrices(String symbol, String period, String from, String to) {
        String token = resolveToken(symbol);
        String key = token + ":" + period;
        long fromEpoch = parseDate(from);
        long toEpoch = parseDate(to);
        logger.debug("Fetching prices for {} {} from {} to {}", token, period, from, to);

        try {
            NavigableMap<Long, Double> map = memoryCache.get(key);
            if (map != null) {
                logger.debug("Returning prices from memory cache for {}", key);
                return new ArrayList<>(map.subMap(fromEpoch, true, toEpoch, true).values());
            }

            if (jedis.exists(key)) {
                logger.debug("Returning prices from redis cache for {}", key);
                Set<Tuple> tuples = jedis.zrangeByScoreWithScores(key, fromEpoch, toEpoch);
                if (!tuples.isEmpty()) {
                    List<Double> prices = new ArrayList<>();
                    for (Tuple t : tuples) {
                        prices.add(Double.parseDouble(t.getElement()));
                    }
                    return prices;
                }
            }
        } catch (JedisConnectionException e) {
            logger.error("Redis not available", e);
            throw new IllegalStateException("Redis not available", e);
        }

        List<Candle> candles = fetchFromKite(token, period, from, to);

        List<Double> prices = new ArrayList<>();
        if (candles != null) {
            try {
                logger.debug("Caching {} candles for {}", candles.size(), key);
                NavigableMap<Long, Double> map = memoryCache.computeIfAbsent(key, k -> new TreeMap<>());
                for (Candle c : candles) {
                    jedis.zadd(key, c.timestamp, String.valueOf(c.close));
                    map.put(c.timestamp, c.close);
                    if (c.timestamp >= fromEpoch && c.timestamp <= toEpoch) {
                        prices.add(c.close);
                    }
                }
            } catch (JedisConnectionException e) {
                logger.error("Redis not available", e);
                throw new IllegalStateException("Redis not available", e);
            }
        }
        return prices;
    }

    private static Set<String> loadNiftySymbols() {
        Set<String> set = new HashSet<>();
        try {
            ClassPathResource res = new ClassPathResource("nifty500.txt");
            InputStream is = res.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) set.add(line);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load nifty500 list", e);
        }
        return set;
    }

    protected List<Candle> fetchFromKite(String symbol, String period, String from, String to) {
        String apiKey = Config.get("kite_api_key");
        String accessToken = RedisStore.get("kite_access_token");
        if (accessToken == null || accessToken.isEmpty()) {
            return new ArrayList<>();
        }
        String url = String.format(
                "https://api.kite.trade/instruments/historical/%s/%s?from=%s&to=%s",
                symbol, period, from, to);
        try {
            logger.debug("Requesting data from KITE: {}", url);
            URL u = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) u.openConnection();
            conn.setRequestProperty("X-Kite-Version", "3");
            conn.setRequestProperty("Authorization", String.format("token %s:%s", apiKey, accessToken));
            conn.setRequestMethod("GET");
            if (conn.getResponseCode() != 200) {
                return new ArrayList<>();
            }
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(conn.getInputStream());
            List<Candle> list = new ArrayList<>();
            for (JsonNode candle : root.path("data").path("candles")) {
                String ts = candle.get(0).asText();
                double close = candle.get(4).asDouble();
                list.add(new Candle(parseTimestamp(ts), close));
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
                instrumentCache = filterNifty(mapper.readValue(json, List.class));
                return instrumentCache;
            }
        } catch (JedisConnectionException | IOException e) {
            logger.error("Redis not available", e);
        }
        List<Map<String, String>> data = fetchInstrumentDump();
        if (data != null) {
            data = filterNifty(data);
            try {
                jedis.set("instruments", new ObjectMapper().writeValueAsString(data));
            } catch (JedisConnectionException | IOException e) {
                logger.error("Redis not available", e);
            }
        }
        instrumentCache = data;
        return data;
    }

    private List<Map<String, String>> filterNifty(List<Map<String, String>> list) {
        if (list == null) return new ArrayList<>();
        return list.stream()
                .filter(m -> NIFTY_SYMBOLS.contains(m.get("tradingsymbol")))
                .collect(Collectors.toList());
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

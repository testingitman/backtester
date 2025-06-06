package com.backtester.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.resps.Tuple;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class QuoteServiceTest {

    @Test
    void returnsPricesFromRedisRange() {
        Jedis jedis = mock(Jedis.class);
        List<Tuple> tuples = new ArrayList<>();
        tuples.add(new Tuple("100", 1.0));
        tuples.add(new Tuple("101", 2.0));
        when(jedis.exists("123:1d")).thenReturn(true);
        when(jedis.zrangeByScoreWithScores("123:1d", 1, 2)).thenReturn(tuples);

        QuoteService service = new QuoteService(jedis);
        List<Double> prices = service.getPrices("123", "1d", "1970-01-01", "1970-01-01");

        assertEquals(List.of(100.0, 101.0), prices);
        verify(jedis).zrangeByScoreWithScores("123:1d", 1, 2);
    }

    @Test
    void fetchesFromKiteWhenCacheEmpty() {
        Jedis jedis = mock(Jedis.class);
        when(jedis.exists(anyString())).thenReturn(false);
        when(jedis.zrangeByScoreWithScores(anyString(), anyDouble(), anyDouble())).thenReturn(List.of());

        QuoteService service = Mockito.spy(new QuoteService(jedis));
        List<QuoteService.Candle> candles = List.of(new QuoteService.Candle(1L, 100.0));
        doReturn(candles).when(service).fetchFromKite(anyString(), anyString(), anyString(), anyString());

        List<Double> prices = service.getPrices("123", "1d", "1970-01-01", "1970-01-01");

        assertEquals(List.of(100.0), prices);
        verify(service).fetchFromKite("123", "1d", "1970-01-01", "1970-01-01");
        verify(jedis, atLeastOnce()).zadd(eq("123:1d"), eq(1.0), eq("100.0"));
    }
}

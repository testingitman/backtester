package com.backtester.service;

import com.backtester.strategy.*;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class StrategyFactory {
    private final Map<String, Strategy> strategies = new HashMap<>();

    public StrategyFactory() {
        register(new RSIStrategy());
        register(new MACDStrategy());
        register(new BollingerStrategy());
        register(new SupertrendStrategy());
    }

    private void register(Strategy s) {
        strategies.put(s.getName().toLowerCase(), s);
    }

    public Strategy getStrategy(String name) {
        return strategies.get(name.toLowerCase());
    }
}

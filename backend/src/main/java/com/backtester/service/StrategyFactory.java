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
        register(new RSISMACrossoverStrategy());
        register(new MACDCrossStrategy());
        register(new BollingerReversalStrategy());
        register(new GoldenCrossStrategy());
        register(new BreakoutStrategy());
        register(new ADXDIStrategy());
        register(new VWAPPullbackStrategy());
        register(new RSIEMABounceStrategy());
        register(new MACDHistogramStrategy());
        register(new OpeningRangeBreakoutStrategy());
        register(new ScalpingSupertrendStrategy());
        register(new MovingAverageRibbonStrategy());
        register(new MeanReversionVWAPStrategy());
    }

    private void register(Strategy s) {
        strategies.put(s.getName().toLowerCase(), s);
    }

    public Strategy getStrategy(String name) {
        return strategies.get(name.toLowerCase());
    }
}

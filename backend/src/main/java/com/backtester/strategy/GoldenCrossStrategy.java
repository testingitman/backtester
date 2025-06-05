package com.backtester.strategy;

import java.util.List;

import static com.backtester.strategy.IndicatorUtils.sma;

public class GoldenCrossStrategy implements Strategy {
    @Override
    public String getName() { return "Golden_Cross"; }

    @Override
    public BacktestResult run(List<Double> prices, double initialCapital) {
        int fast = 50;
        int slow = 200;
        if (prices.size() < slow) return new BacktestResult(initialCapital, 0);

        double cash = initialCapital;
        int position = 0;
        double peak = initialCapital;
        double maxDD = 0.0;

        for (int i = slow; i < prices.size(); i++) {
            double fastMa = sma(prices, i, fast);
            double slowMa = sma(prices, i, slow);
            double price = prices.get(i);
            if (fastMa > slowMa && position == 0) {
                position = 1;
                cash -= price;
            } else if (fastMa < slowMa && position == 1) {
                position = 0;
                cash += price;
            }
            double equity = cash + position * price;
            if (equity > peak) peak = equity;
            double dd = (peak - equity)/peak;
            if (dd > maxDD) maxDD = dd;
        }
        double finalCap = cash + position * prices.get(prices.size()-1);
        return new BacktestResult(finalCap, maxDD);
    }
}

package com.backtester.strategy;

import java.util.List;

import static com.backtester.strategy.IndicatorUtils.sma;

public class SupertrendStrategy implements Strategy {
    @Override
    public String getName() { return "Supertrend"; }

    @Override
    public BacktestResult run(List<Double> prices, double initialCapital) {
        int fast = 10;
        int slow = 30;
        if (prices.size() < slow) {
            return new BacktestResult(initialCapital, 0);
        }

        double cash = initialCapital;
        int position = 0;
        double peak = initialCapital;
        double maxDrawdown = 0.0;

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
            double dd = (peak - equity) / peak;
            if (dd > maxDrawdown) maxDrawdown = dd;
        }

        double finalCapital = cash + position * prices.get(prices.size() - 1);
        return new BacktestResult(finalCapital, maxDrawdown);
    }
}

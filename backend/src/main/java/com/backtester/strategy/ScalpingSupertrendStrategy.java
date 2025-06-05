package com.backtester.strategy;

import java.util.ArrayList;
import java.util.List;

import static com.backtester.strategy.IndicatorUtils.sma;

public class ScalpingSupertrendStrategy implements Strategy {
    @Override
    public String getName() { return "Scalp_Supertrend"; }

    @Override
    public BacktestResult run(List<Double> prices, double initialCapital) {
        int fast = 5;
        int slow = 10;
        if (prices.size() < slow)
            return new BacktestResult(initialCapital, initialCapital, 0, prices, new ArrayList<>());
        double cash = initialCapital;
        int position = 0;
        double peak = initialCapital;
        double maxDD = 0.0;
        List<Trade> trades = new ArrayList<>();
        for (int i = slow; i < prices.size(); i++) {
            double fastMa = sma(prices, i, fast);
            double slowMa = sma(prices, i, slow);
            double price = prices.get(i);
            if (fastMa > slowMa && position == 0) {
                position = 1;
                cash -= price;
                trades.add(new Trade(i, price, "BUY"));
            } else if (fastMa < slowMa && position == 1) {
                position = 0;
                cash += price;
                trades.add(new Trade(i, price, "SELL"));
            }
            double equity = cash + position * price;
            if (equity > peak) peak = equity;
            double dd = (peak - equity)/peak;
            if (dd > maxDD) maxDD = dd;
        }
        double finalCap = cash + position * prices.get(prices.size()-1);
        return new BacktestResult(initialCapital, finalCap, maxDD, prices, trades);
    }
}

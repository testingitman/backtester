package com.backtester.strategy;

import java.util.List;

import static com.backtester.strategy.IndicatorUtils.highest;
import static com.backtester.strategy.IndicatorUtils.lowest;

public class OpeningRangeBreakoutStrategy implements Strategy {
    @Override
    public String getName() { return "Opening_Range"; }

    @Override
    public BacktestResult run(List<Double> prices, double initialCapital) {
        int rangePeriod = 30; // first 30 bars
        if (prices.size() <= rangePeriod) return new BacktestResult(initialCapital, 0);
        double high = highest(prices, rangePeriod - 1, rangePeriod);
        double low = lowest(prices, rangePeriod - 1, rangePeriod);

        double cash = initialCapital;
        int position = 0;
        double peak = initialCapital;
        double maxDD = 0.0;

        for (int i = rangePeriod; i < prices.size(); i++) {
            double price = prices.get(i);
            if (price > high && position == 0) {
                position = 1;
                cash -= price;
            } else if (price < low && position == 1) {
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

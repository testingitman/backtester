package com.backtester.strategy;

import java.util.List;

import static com.backtester.strategy.IndicatorUtils.vwap;

public class MeanReversionVWAPStrategy implements Strategy {
    @Override
    public String getName() { return "Mean_VWAP"; }

    @Override
    public BacktestResult run(List<Double> prices, double initialCapital) {
        int lookback = 20;
        double cash = initialCapital;
        int position = 0;
        double peak = initialCapital;
        double maxDD = 0.0;

        for (int i = lookback; i < prices.size(); i++) {
            double vw = vwap(prices, i, lookback);
            double price = prices.get(i);
            if (price < vw * 0.98 && position == 0) {
                position = 1;
                cash -= price;
            } else if (price > vw * 1.02 && position == 1) {
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

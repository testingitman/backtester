package com.backtester.strategy;

import java.util.List;

import static com.backtester.strategy.IndicatorUtils.ema;

public class MACDHistogramStrategy implements Strategy {
    @Override
    public String getName() { return "MACD_Hist"; }

    @Override
    public BacktestResult run(List<Double> prices, double initialCapital) {
        if (prices.size() < 35) return new BacktestResult(initialCapital, 0);

        List<Double> ema12 = ema(prices, 12);
        List<Double> ema26 = ema(prices, 26);
        List<Double> macd = new java.util.ArrayList<>();
        for (int i = 0; i < prices.size(); i++) {
            macd.add(ema12.get(i) - ema26.get(i));
        }
        List<Double> signal = ema(macd, 9);

        double cash = initialCapital;
        int position = 0;
        double peak = initialCapital;
        double maxDD = 0.0;

        for (int i = 1; i < prices.size(); i++) {
            double histPrev = macd.get(i-1) - signal.get(i-1);
            double hist = macd.get(i) - signal.get(i);
            double price = prices.get(i);
            if (hist > 0 && histPrev <= 0 && position == 0) {
                position = 1;
                cash -= price;
            } else if (hist < 0 && histPrev >= 0 && position == 1) {
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

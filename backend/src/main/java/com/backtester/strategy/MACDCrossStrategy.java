package com.backtester.strategy;

import java.util.ArrayList;
import java.util.List;

import static com.backtester.strategy.IndicatorUtils.ema;

public class MACDCrossStrategy implements Strategy {
    @Override
    public String getName() { return "MACD_CROSS"; }

    @Override
    public BacktestResult run(List<Double> prices, double initialCapital) {
        if (prices.size() < 35)
            return new BacktestResult(initialCapital, initialCapital, 0, prices, new ArrayList<>());

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
        List<Trade> trades = new ArrayList<>();
        for (int i = 1; i < prices.size(); i++) {
            double prev = macd.get(i-1) - signal.get(i-1);
            double cur = macd.get(i) - signal.get(i);
            double price = prices.get(i);
            if (cur > 0 && prev <= 0 && position == 0) {
                position = 1;
                cash -= price;
                trades.add(new Trade(i, price, "BUY"));
            } else if (cur < 0 && prev >= 0 && position == 1) {
                position = 0;
                cash += price;
                trades.add(new Trade(i, price, "SELL"));
            }
            double equity = cash + position * price;
            if (equity > peak) peak = equity;
            double dd = (peak - equity) / peak;
            if (dd > maxDD) maxDD = dd;
        }
        double finalCap = cash + position * prices.get(prices.size()-1);
        return new BacktestResult(initialCapital, finalCap, maxDD, prices, trades);
    }
}

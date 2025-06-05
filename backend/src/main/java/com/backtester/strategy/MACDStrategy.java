package com.backtester.strategy;

import java.util.List;

import static com.backtester.strategy.IndicatorUtils.ema;

public class MACDStrategy implements Strategy {
    @Override
    public String getName() { return "MACD"; }

    @Override
    public BacktestResult run(List<Double> prices, double initialCapital) {
        if (prices.size() < 35) {
            return new BacktestResult(initialCapital, 0);
        }

        List<Double> ema12 = ema(prices, 12);
        List<Double> ema26 = ema(prices, 26);

        double cash = initialCapital;
        int position = 0;
        double peak = initialCapital;
        double maxDrawdown = 0.0;
        double prevMacd = 0;
        double prevSignal = 0;

        // compute MACD and signal on the fly
        List<Double> macdValues = new java.util.ArrayList<>();
        for (int i = 0; i < prices.size(); i++) {
            macdValues.add(ema12.get(i) - ema26.get(i));
        }
        List<Double> signal = ema(macdValues, 9);

        for (int i = 1; i < prices.size(); i++) {
            double macd = macdValues.get(i);
            double sig = signal.get(i);
            double price = prices.get(i);

            if (macd > sig && prevMacd <= prevSignal && position == 0) {
                position = 1;
                cash -= price;
            } else if (macd < sig && prevMacd >= prevSignal && position == 1) {
                position = 0;
                cash += price;
            }

            double equity = cash + position * price;
            if (equity > peak) peak = equity;
            double dd = (peak - equity) / peak;
            if (dd > maxDrawdown) maxDrawdown = dd;

            prevMacd = macd;
            prevSignal = sig;
        }

        double finalCapital = cash + position * prices.get(prices.size() - 1);
        return new BacktestResult(finalCapital, maxDrawdown);
    }
}

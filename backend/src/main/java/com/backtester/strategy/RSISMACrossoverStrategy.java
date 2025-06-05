package com.backtester.strategy;

import java.util.List;

import static com.backtester.strategy.IndicatorUtils.*;

public class RSISMACrossoverStrategy implements Strategy {
    @Override
    public String getName() { return "RSI_SMA"; }

    @Override
    public BacktestResult run(List<Double> prices, double initialCapital) {
        int rsiPeriod = 14;
        int smaPeriod = 50;
        double cash = initialCapital;
        int position = 0;
        double peak = initialCapital;
        double maxDD = 0.0;

        for (int i = Math.max(rsiPeriod, smaPeriod); i < prices.size(); i++) {
            double r = rsi(prices, i, rsiPeriod);
            double ma = sma(prices, i, smaPeriod);
            double price = prices.get(i);

            if (r < 30 && price > ma && position == 0) {
                position = 1;
                cash -= price;
            } else if ((r > 70 || price < ma) && position == 1) {
                position = 0;
                cash += price;
            }

            double equity = cash + position * price;
            if (equity > peak) peak = equity;
            double dd = (peak - equity) / peak;
            if (dd > maxDD) maxDD = dd;
        }
        double finalCap = cash + position * prices.get(prices.size() - 1);
        return new BacktestResult(finalCap, maxDD);
    }
}

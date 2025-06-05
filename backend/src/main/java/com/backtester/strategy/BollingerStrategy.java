package com.backtester.strategy;

import java.util.List;

import static com.backtester.strategy.IndicatorUtils.sma;

public class BollingerStrategy implements Strategy {
    @Override
    public String getName() { return "BollingerBands"; }

    @Override
    public BacktestResult run(List<Double> prices, double initialCapital) {
        int period = 20;
        double cash = initialCapital;
        int position = 0;
        double peak = initialCapital;
        double maxDrawdown = 0.0;

        for (int i = period; i < prices.size(); i++) {
            double ma = sma(prices, i, period);
            double sd = 0.0;
            for (int j = i - period + 1; j <= i; j++) {
                double diff = prices.get(j) - ma;
                sd += diff * diff;
            }
            sd = Math.sqrt(sd / period);
            double upper = ma + 2 * sd;
            double lower = ma - 2 * sd;
            double price = prices.get(i);

            if (price < lower && position == 0) {
                position = 1;
                cash -= price;
            } else if (price > upper && position == 1) {
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

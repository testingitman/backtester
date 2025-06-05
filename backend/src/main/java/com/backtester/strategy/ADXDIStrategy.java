package com.backtester.strategy;

import java.util.List;

import static com.backtester.strategy.IndicatorUtils.*;

public class ADXDIStrategy implements Strategy {
    @Override
    public String getName() { return "ADX_DI"; }

    @Override
    public BacktestResult run(List<Double> prices, double initialCapital) {
        int period = 14;
        double cash = initialCapital;
        int position = 0;
        double peak = initialCapital;
        double maxDD = 0.0;

        for (int i = period; i < prices.size(); i++) {
            double adxVal = adx(prices, i, period);
            double up = highest(prices, i, 1) - prices.get(i-1);
            double down = prices.get(i-1) - lowest(prices, i, 1);
            boolean diPlus = up > down;
            double price = prices.get(i);

            if (adxVal > 25 && diPlus && position == 0) {
                position = 1;
                cash -= price;
            } else if (adxVal > 25 && !diPlus && position == 1) {
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

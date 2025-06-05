package com.backtester.strategy;

import java.util.ArrayList;
import java.util.List;

import static com.backtester.strategy.IndicatorUtils.rsi;

public class RSIStrategy implements Strategy {
    @Override
    public String getName() { return "RSI"; }

    @Override
    public BacktestResult run(List<Double> prices, double initialCapital) {
        int period = 14;
        double cash = initialCapital;
        int position = 0;
        double peak = initialCapital;
        double maxDrawdown = 0.0;
        List<Trade> trades = new ArrayList<>();

        for (int i = period; i < prices.size(); i++) {
            double price = prices.get(i);
            double r = rsi(prices, i, period);
            if (r < 30 && position == 0) {
                position = 1;
                cash -= price;
                trades.add(new Trade(i, price, "BUY"));
            } else if (r > 70 && position == 1) {
                position = 0;
                cash += price;
                trades.add(new Trade(i, price, "SELL"));
            }
            double equity = cash + position * price;
            if (equity > peak) {
                peak = equity;
            }
            double dd = (peak - equity) / peak;
            if (dd > maxDrawdown) {
                maxDrawdown = dd;
            }
        }

        double finalCapital = cash + position * prices.get(prices.size() - 1);
        return new BacktestResult(initialCapital, finalCapital, maxDrawdown, prices, trades);
    }
}

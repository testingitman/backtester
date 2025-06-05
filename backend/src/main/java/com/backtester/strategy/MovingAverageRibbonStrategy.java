package com.backtester.strategy;

import java.util.ArrayList;
import java.util.List;

import static com.backtester.strategy.IndicatorUtils.ema;

public class MovingAverageRibbonStrategy implements Strategy {
    @Override
    public String getName() { return "MA_Ribbon"; }

    @Override
    public BacktestResult run(List<Double> prices, double initialCapital) {
        int[] periods = {8, 13, 21};
        List<Double> ema1 = ema(prices, periods[0]);
        List<Double> ema2 = ema(prices, periods[1]);
        List<Double> ema3 = ema(prices, periods[2]);

        int start = periods[2];
        double cash = initialCapital;
        int position = 0;
        double peak = initialCapital;
        double maxDD = 0.0;
        List<Trade> trades = new ArrayList<>();

        for (int i = start; i < prices.size(); i++) {
            double price = prices.get(i);
            if (ema1.get(i) > ema2.get(i) && ema2.get(i) > ema3.get(i) && position == 0) {
                position = 1;
                cash -= price;
                trades.add(new Trade(i, price, "BUY"));
            } else if (!(ema1.get(i) > ema2.get(i) && ema2.get(i) > ema3.get(i)) && position == 1) {
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

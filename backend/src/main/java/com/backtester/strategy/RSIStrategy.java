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
        List<Double> equity = new ArrayList<>();
        Trade openTrade = null;

        for (int i = period; i < prices.size(); i++) {
            double price = prices.get(i);
            double r = rsi(prices, i, period);
            if (r < 30 && position == 0) {
                position = 1;
                cash -= price;
                openTrade = new Trade(i, price);
            } else if (r > 70 && position == 1) {
                position = 0;
                cash += price;
                if (openTrade != null) {
                    openTrade.setExit(i, price);
                    trades.add(openTrade);
                    openTrade = null;
                }
            }
            double eq = cash + position * price;
            equity.add(eq);
            if (eq > peak) {
                peak = eq;
            }
            double dd = (peak - eq) / peak;
            if (dd > maxDrawdown) {
                maxDrawdown = dd;
            }
        }

        double finalCapital = cash + position * prices.get(prices.size() - 1);
        if (openTrade != null) {
            openTrade.setExit(prices.size() - 1, prices.get(prices.size() - 1));
            trades.add(openTrade);
        }
        BacktestResult result = new BacktestResult(finalCapital, maxDrawdown);
        result.setTrades(trades);
        result.setEquityCurve(equity);
        result.setInitialCapital(initialCapital);
        return result;
    }
}

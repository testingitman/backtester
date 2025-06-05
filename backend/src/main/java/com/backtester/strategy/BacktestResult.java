package com.backtester.strategy;

import java.util.List;

public class BacktestResult {
    private double initialCapital;
    private double finalCapital;
    private double maxDrawdown;
    private List<Double> prices;
    private List<Trade> trades;

    public BacktestResult(double initialCapital, double finalCapital, double maxDrawdown,
                          List<Double> prices, List<Trade> trades) {
        this.initialCapital = initialCapital;
        this.finalCapital = finalCapital;
        this.maxDrawdown = maxDrawdown;
        this.prices = prices;
        this.trades = trades;
    }

    public double getInitialCapital() {
        return initialCapital;
    }

    public double getFinalCapital() {
        return finalCapital;
    }

    public double getMaxDrawdown() {
        return maxDrawdown;
    }

    public List<Double> getPrices() {
        return prices;
    }

    public List<Trade> getTrades() {
        return trades;
    }
}

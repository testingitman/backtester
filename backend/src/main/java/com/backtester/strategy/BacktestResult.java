package com.backtester.strategy;

public class BacktestResult {
    private double finalCapital;
    private double maxDrawdown;

    public BacktestResult(double finalCapital, double maxDrawdown) {
        this.finalCapital = finalCapital;
        this.maxDrawdown = maxDrawdown;
    }

    public double getFinalCapital() {
        return finalCapital;
    }

    public double getMaxDrawdown() {
        return maxDrawdown;
    }
}

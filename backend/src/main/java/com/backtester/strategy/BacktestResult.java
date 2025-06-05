package com.backtester.strategy;

import com.backtester.model.Candle;

import java.util.ArrayList;
import java.util.List;

public class BacktestResult {
    private double initialCapital;
    private double finalCapital;
    private double maxDrawdown;
    private List<Candle> candles = new ArrayList<>();
    private List<Trade> trades = new ArrayList<>();
    private List<Double> equityCurve = new ArrayList<>();

    public BacktestResult(double finalCapital, double maxDrawdown) {
        this.finalCapital = finalCapital;
        this.maxDrawdown = maxDrawdown;
    }

    public double getInitialCapital() {
        return initialCapital;
    }

    public void setInitialCapital(double initialCapital) {
        this.initialCapital = initialCapital;
    }

    public double getFinalCapital() {
        return finalCapital;
    }

    public double getProfitPercent() {
        if (initialCapital == 0) return 0;
        return (finalCapital - initialCapital) * 100.0 / initialCapital;
    }

    public double getMaxDrawdown() {
        return maxDrawdown;
    }

    public List<Candle> getCandles() {
        return candles;
    }

    public void setCandles(List<Candle> candles) {
        this.candles = candles;
    }

    public List<Trade> getTrades() {
        return trades;
    }

    public void setTrades(List<Trade> trades) {
        this.trades = trades;
    }

    public List<Double> getEquityCurve() {
        return equityCurve;
    }

    public void setEquityCurve(List<Double> equityCurve) {
        this.equityCurve = equityCurve;
    }
}

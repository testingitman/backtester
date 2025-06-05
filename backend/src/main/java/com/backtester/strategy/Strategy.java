package com.backtester.strategy;

import java.util.List;

public interface Strategy {
    String getName();
    BacktestResult run(List<Double> prices, double initialCapital);
}

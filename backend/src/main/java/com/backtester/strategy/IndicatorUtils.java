package com.backtester.strategy;

import java.util.ArrayList;
import java.util.List;

public class IndicatorUtils {
    public static double sma(List<Double> prices, int index, int period) {
        double sum = 0.0;
        for (int i = index - period + 1; i <= index; i++) {
            sum += prices.get(i);
        }
        return sum / period;
    }

    public static List<Double> ema(List<Double> prices, int period) {
        List<Double> out = new ArrayList<>();
        double k = 2.0 / (period + 1);
        double prev = prices.get(0);
        out.add(prev);
        for (int i = 1; i < prices.size(); i++) {
            prev = prices.get(i) * k + prev * (1 - k);
            out.add(prev);
        }
        return out;
    }

    public static double rsi(List<Double> prices, int index, int period) {
        double gain = 0.0;
        double loss = 0.0;
        for (int i = index - period + 1; i <= index; i++) {
            double diff = prices.get(i) - prices.get(i - 1);
            if (diff > 0) gain += diff; else loss -= diff;
        }
        double avgGain = gain / period;
        double avgLoss = loss / period;
        if (avgLoss == 0) {
            return 100.0;
        }
        double rs = avgGain / avgLoss;
        return 100.0 - (100.0 / (1 + rs));
    }
}

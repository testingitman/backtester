package com.backtester.model;

public class Candle {
    private final String time;
    private final double open;
    private final double high;
    private final double low;
    private final double close;
    private final double volume;

    public Candle(String time, double open, double high, double low, double close, double volume) {
        this.time = time;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
    }

    public String getTime() { return time; }
    public double getOpen() { return open; }
    public double getHigh() { return high; }
    public double getLow() { return low; }
    public double getClose() { return close; }
    public double getVolume() { return volume; }
}

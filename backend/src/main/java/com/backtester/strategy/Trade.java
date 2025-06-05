package com.backtester.strategy;

public class Trade {
    private int entryIndex;
    private double entryPrice;
    private int exitIndex;
    private double exitPrice;

    public Trade(int entryIndex, double entryPrice) {
        this.entryIndex = entryIndex;
        this.entryPrice = entryPrice;
    }

    public void setExit(int exitIndex, double exitPrice) {
        this.exitIndex = exitIndex;
        this.exitPrice = exitPrice;
    }

    public int getEntryIndex() { return entryIndex; }
    public double getEntryPrice() { return entryPrice; }
    public int getExitIndex() { return exitIndex; }
    public double getExitPrice() { return exitPrice; }
}

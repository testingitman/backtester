package com.backtester.strategy;

public class Trade {
    private int index;
    private double price;
    private String side;

    public Trade(int index, double price, String side) {
        this.index = index;
        this.price = price;
        this.side = side;
    }

    public int getIndex() {
        return index;
    }

    public double getPrice() {
        return price;
    }

    public String getSide() {
        return side;
    }
}

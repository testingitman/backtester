package com.backtester.service;

import com.backtester.strategy.BacktestResult;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;

@Service
public class HistoryService {
    private final List<String> history = new LinkedList<>();

    public void add(String entry) {
        history.add(0, entry);
    }

    public List<String> getHistory() {
        return history;
    }
}

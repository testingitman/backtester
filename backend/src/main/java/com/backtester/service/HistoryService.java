package com.backtester.service;

import com.backtester.strategy.BacktestResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;

@Service
public class HistoryService {
    private final List<String> history = new LinkedList<>();
    private static final Logger logger = LoggerFactory.getLogger(HistoryService.class);

    public void add(String entry) {
        logger.debug("Adding history entry: {}", entry);
        history.add(0, entry);
    }

    public List<String> getHistory() {
        logger.debug("Returning {} history entries", history.size());
        return history;
    }
}

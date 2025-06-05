package com.backtester.controller;

import com.backtester.service.HistoryService;
import com.backtester.service.QuoteService;
import com.backtester.service.StrategyFactory;
import com.backtester.strategy.BacktestResult;
import com.backtester.strategy.Strategy;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/backtest")
public class BacktestController {
    private final QuoteService quoteService;
    private final StrategyFactory strategyFactory;
    private final HistoryService historyService;

    public BacktestController(QuoteService quoteService, StrategyFactory strategyFactory, HistoryService historyService) {
        this.quoteService = quoteService;
        this.strategyFactory = strategyFactory;
        this.historyService = historyService;
    }

    @GetMapping
    public BacktestResult run(@RequestParam String strategy,
                              @RequestParam String symbol,
                              @RequestParam String period,
                              @RequestParam String from,
                              @RequestParam String to,
                              @RequestParam double capital) {
        Strategy strat = strategyFactory.getStrategy(strategy);
        if (strat == null) {
            throw new IllegalArgumentException("Unknown strategy: " + strategy);
        }
        List<Double> prices = quoteService.getPrices(symbol, period, from, to);
        BacktestResult result = strat.run(prices, capital);
        double profitPct = (result.getFinalCapital() - result.getInitialCapital()) / result.getInitialCapital() * 100.0;
        historyService.add(String.format("%s %s %.2f%%", strategy, symbol, profitPct));
        return result;
    }

    @GetMapping("/history")
    public List<String> history() {
        return historyService.getHistory();
    }
}

package com.backtester.controller;

import com.backtester.service.HistoryService;
import com.backtester.service.QuoteService;
import com.backtester.service.StrategyFactory;
import com.backtester.strategy.BacktestResult;
import com.backtester.strategy.Strategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/backtest")
public class BacktestController {
    private final QuoteService quoteService;
    private final StrategyFactory strategyFactory;
    private final HistoryService historyService;
    private static final Logger logger = LoggerFactory.getLogger(BacktestController.class);

    public BacktestController(QuoteService quoteService, StrategyFactory strategyFactory, HistoryService historyService) {
        this.quoteService = quoteService;
        this.strategyFactory = strategyFactory;
        this.historyService = historyService;
    }

    @GetMapping
    public ResponseEntity<?> run(@RequestParam String strategy,
                                 @RequestParam String symbol,
                                 @RequestParam String period,
                                 @RequestParam(required = false, name = "from") String from,
                                 @RequestParam(required = false, name = "start") String start,
                                 @RequestParam(required = false, name = "to") String to,
                                 @RequestParam(required = false, name = "end") String end,
                                 @RequestParam double capital) {
        logger.debug("Backtest request: strategy={} symbol={} period={} from={} to={} capital={}",
                strategy, symbol, period, from != null ? from : start, to != null ? to : end, capital);
        if (from == null) from = start;
        if (to == null) to = end;
        if (from == null || to == null) {
            return ResponseEntity.badRequest().body(
                    java.util.Collections.singletonMap("error", "Date range required"));
        }
        Strategy strat = strategyFactory.getStrategy(strategy);
        if (strat == null) {
            return ResponseEntity.badRequest().body(
                    java.util.Collections.singletonMap("error", "Unknown strategy: " + strategy));
        }
        try {
            List<Double> prices = quoteService.getPrices(symbol, period, from, to);
            BacktestResult result = strat.run(prices, capital);
            double profitPct = (result.getFinalCapital() - result.getInitialCapital()) / result.getInitialCapital() * 100.0;
            historyService.add(String.format("%s %s %.2f%%", strategy, symbol, profitPct));
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            logger.error("Redis access error", e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(java.util.Map.of("error", "Redis not available", "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error running backtest", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/history")
    public List<String> history() {
        return historyService.getHistory();
    }
}

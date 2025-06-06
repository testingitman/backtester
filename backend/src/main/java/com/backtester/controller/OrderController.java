package com.backtester.controller;

import com.backtester.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/order")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/funds")
    public Map<String, Double> funds() {
        double f = orderService.getFunds();
        return Map.of("funds", f);
    }

    @PostMapping
    public ResponseEntity<?> place(@RequestBody Map<String, Object> payload) {
        String symbol = (String) payload.get("symbol");
        String exchange = (String) payload.getOrDefault("exchange", "NSE");
        String action = (String) payload.getOrDefault("action", "BUY");
        int qty = ((Number) payload.getOrDefault("quantity", 0)).intValue();
        double price = ((Number) payload.getOrDefault("price", 0)).doubleValue();
        return ResponseEntity.ok(orderService.placeOrder(symbol, exchange, action, qty, price));
    }
}

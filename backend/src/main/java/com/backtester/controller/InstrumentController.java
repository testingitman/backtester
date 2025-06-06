package com.backtester.controller;

import com.backtester.service.QuoteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/instruments")
public class InstrumentController {
    private final QuoteService quoteService;

    public InstrumentController(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, String>>> list() {
        return ResponseEntity.ok(quoteService.getInstruments());
    }
}

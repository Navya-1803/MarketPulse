package com.marketpulse.market.controller;

import com.marketpulse.market.dto.MarketQuoteDto;
import com.marketpulse.market.service.MarketDataService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/market")
public class MarketController {

    private final MarketDataService marketDataService;

    public MarketController(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    @GetMapping("/{symbol}")
    public MarketQuoteDto get(@PathVariable String symbol) {
        return marketDataService.getQuote(symbol.toUpperCase());
    }
}

package com.marketpulse.market.controller;

import com.marketpulse.market.dto.MarketQuoteDto;
import com.marketpulse.market.service.MarketDataService;
import com.marketpulse.market.service.StockCatalogService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stocks")
public class StocksController {

    private final StockCatalogService stockCatalogService;
    private final MarketDataService marketDataService;

    public StocksController(StockCatalogService stockCatalogService, MarketDataService marketDataService) {
        this.stockCatalogService = stockCatalogService;
        this.marketDataService = marketDataService;
    }

    @GetMapping
    public List<MarketQuoteDto> list(@RequestParam(required = false) String query) {
        return stockCatalogService.getCatalogQuotes(query);
    }

    @GetMapping("/search")
    public List<MarketQuoteDto> search(@RequestParam(required = false, defaultValue = "") String query) {
        return stockCatalogService.getCatalogQuotes(query);
    }

    @GetMapping("/{symbol}")
    public MarketQuoteDto get(@PathVariable String symbol) {
        return marketDataService.getQuote(symbol.toUpperCase());
    }
}

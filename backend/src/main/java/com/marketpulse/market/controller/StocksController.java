package com.marketpulse.market.controller;

import com.marketpulse.common.exception.ResourceNotFoundException;
import com.marketpulse.common.response.PageResponse;
import com.marketpulse.market.dto.MarketQuoteDto;
import com.marketpulse.market.dto.StockHistoryResponse;
import com.marketpulse.market.service.MarketDataService;
import com.marketpulse.market.service.StockCatalogService;
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
    public PageResponse<MarketQuoteDto> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "ALL") String filter,
            @RequestParam(defaultValue = "symbol") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection
    ) {
        return stockCatalogService.getPaginatedQuotes(page, size, query, filter, sortBy, sortDirection);
    }

    @GetMapping("/search")
    public PageResponse<MarketQuoteDto> search(
            @RequestParam(required = false, defaultValue = "") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "ALL") String filter,
            @RequestParam(defaultValue = "symbol") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection
    ) {
        return stockCatalogService.getPaginatedQuotes(page, size, query, filter, sortBy, sortDirection);
    }

    @GetMapping("/{symbol}")
    public MarketQuoteDto get(@PathVariable String symbol) {
        return marketDataService.getQuote(symbol.toUpperCase());
    }

    @GetMapping("/{symbol}/history")
    public StockHistoryResponse history(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "1D") String range
    ) {
        return marketDataService.getStockHistory(symbol.toUpperCase(), range)
                .orElseThrow(() -> new ResourceNotFoundException("Historical market data unavailable for " + symbol));
    }
}

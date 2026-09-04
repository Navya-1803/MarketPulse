package com.marketpulse.market.service;

import com.marketpulse.market.dto.MarketQuoteDto;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class StockCatalogService {

    public record CatalogStock(String symbol, String name, String sector) {
    }

    private static final List<CatalogStock> CATALOG = List.of(
            new CatalogStock("NVDA", "NVIDIA Corporation", "Semiconductors"),
            new CatalogStock("AAPL", "Apple Inc.", "Consumer Electronics"),
            new CatalogStock("MSFT", "Microsoft Corporation", "Software & Cloud"),
            new CatalogStock("GOOGL", "Alphabet Inc.", "Internet & Technology"),
            new CatalogStock("AMZN", "Amazon.com Inc.", "E-Commerce & Cloud"),
            new CatalogStock("META", "Meta Platforms, Inc.", "Social Media"),
            new CatalogStock("TSLA", "Tesla, Inc.", "Automotive & Clean Energy"),
            new CatalogStock("NFLX", "Netflix, Inc.", "Streaming & Media"),
            new CatalogStock("AMD", "Advanced Micro Devices, Inc.", "Semiconductors"),
            new CatalogStock("INTC", "Intel Corporation", "Semiconductors"),
            new CatalogStock("JPM", "JPMorgan Chase & Co.", "Financial Services"),
            new CatalogStock("V", "Visa Inc.", "Payment Technology"),
            new CatalogStock("MA", "Mastercard Incorporated", "Payment Technology"),
            new CatalogStock("BAC", "Bank of America Corporation", "Banking & Finance"),
            new CatalogStock("WMT", "Walmart Inc.", "Retail")
    );

    private final MarketDataService marketDataService;

    public StockCatalogService(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    public List<CatalogStock> getCatalog() {
        return CATALOG;
    }

    public Optional<CatalogStock> findBySymbol(String symbol) {
        if (symbol == null) {
            return Optional.empty();
        }
        String clean = symbol.trim().toUpperCase(Locale.ROOT);
        return CATALOG.stream()
                .filter(s -> s.symbol().equalsIgnoreCase(clean))
                .findFirst();
    }

    public List<MarketQuoteDto> getCatalogQuotes(String query) {
        String cleanQuery = query != null ? query.trim().toLowerCase(Locale.ROOT) : "";
        List<CatalogStock> filtered = CATALOG.stream()
                .filter(stock -> cleanQuery.isEmpty()
                        || stock.symbol().toLowerCase(Locale.ROOT).contains(cleanQuery)
                        || stock.name().toLowerCase(Locale.ROOT).contains(cleanQuery))
                .toList();

        List<MarketQuoteDto> quotes = new ArrayList<>(filtered.size());
        for (CatalogStock stock : filtered) {
            MarketQuoteDto quote = marketDataService.getQuote(stock.symbol());
            if (quote.companyName() == null || quote.companyName().isBlank() || quote.companyName().equals(stock.symbol())) {
                quote = new MarketQuoteDto(
                        quote.symbol(),
                        stock.name(),
                        quote.price(),
                        quote.changeAmount(),
                        quote.changePercent(),
                        quote.dayHigh(),
                        quote.dayLow(),
                        quote.volume(),
                        quote.capturedAt(),
                        quote.status(),
                        quote.message()
                );
            }
            quotes.add(quote);
        }
        return quotes;
    }
}

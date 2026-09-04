package com.marketpulse.watchlist.service;

import com.marketpulse.common.exception.DuplicateResourceException;
import com.marketpulse.common.exception.InvalidSymbolException;
import com.marketpulse.common.exception.ResourceNotFoundException;
import com.marketpulse.market.dto.MarketQuoteDto;
import com.marketpulse.market.service.MarketDataService;
import com.marketpulse.user.entity.UserAccount;
import com.marketpulse.user.service.UserService;
import com.marketpulse.watchlist.dto.AddStockRequest;
import com.marketpulse.watchlist.dto.WatchlistRequest;
import com.marketpulse.watchlist.dto.WatchlistResponse;
import com.marketpulse.watchlist.dto.WatchlistStockResponse;
import com.marketpulse.watchlist.entity.Watchlist;
import com.marketpulse.watchlist.entity.WatchlistStock;
import com.marketpulse.watchlist.repository.WatchlistRepository;
import com.marketpulse.watchlist.repository.WatchlistStockRepository;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WatchlistService {

    private final WatchlistRepository watchlistRepository;
    private final WatchlistStockRepository watchlistStockRepository;
    private final UserService userService;
    private final MarketDataService marketDataService;

    public WatchlistService(
            WatchlistRepository watchlistRepository,
            WatchlistStockRepository watchlistStockRepository,
            UserService userService,
            MarketDataService marketDataService
    ) {
        this.watchlistRepository = watchlistRepository;
        this.watchlistStockRepository = watchlistStockRepository;
        this.userService = userService;
        this.marketDataService = marketDataService;
    }

    @Transactional(readOnly = true)
    public List<WatchlistResponse> list(Long userId) {
        return watchlistRepository.findByUserIdOrderByCreatedAtAsc(userId).stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public WatchlistResponse get(Long userId, Long watchlistId) {
        Watchlist watchlist = requireOwned(userId, watchlistId);
        return toDetail(watchlist);
    }

    @Transactional
    public WatchlistResponse create(Long userId, WatchlistRequest request) {
        if (watchlistRepository.existsByUserIdAndNameIgnoreCase(userId, request.name().trim())) {
            throw new DuplicateResourceException("A watchlist with this name already exists");
        }
        UserAccount user = userService.getById(userId);
        Watchlist watchlist = new Watchlist();
        watchlist.setUser(user);
        watchlist.setName(request.name().trim());
        return toSummary(watchlistRepository.save(watchlist));
    }

    @Transactional
    public WatchlistResponse rename(Long userId, Long watchlistId, WatchlistRequest request) {
        Watchlist watchlist = requireOwned(userId, watchlistId);
        String name = request.name().trim();
        if (!watchlist.getName().equalsIgnoreCase(name)
                && watchlistRepository.existsByUserIdAndNameIgnoreCase(userId, name)) {
            throw new DuplicateResourceException("A watchlist with this name already exists");
        }
        watchlist.setName(name);
        return toSummary(watchlist);
    }

    @Transactional
    public void delete(Long userId, Long watchlistId) {
        Watchlist watchlist = requireOwned(userId, watchlistId);
        watchlistRepository.delete(watchlist);
    }

    @Transactional
    public WatchlistResponse addStock(Long userId, Long watchlistId, AddStockRequest request) {
        Watchlist watchlist = requireOwned(userId, watchlistId);
        String symbol = normalizeSymbol(request.symbol());
        if (watchlistStockRepository.existsByWatchlistIdAndSymbol(watchlist.getId(), symbol)) {
            throw new DuplicateResourceException(symbol + " is already in this watchlist");
        }
        WatchlistStock stock = new WatchlistStock();
        stock.setWatchlist(watchlist);
        stock.setSymbol(symbol);
        watchlist.getStocks().add(stock);
        watchlistStockRepository.save(stock);
        return toDetail(watchlist);
    }

    @Transactional
    public void removeStock(Long userId, Long watchlistId, String symbol) {
        Watchlist watchlist = requireOwned(userId, watchlistId);
        String normalized = normalizeSymbol(symbol);
        WatchlistStock stock = watchlistStockRepository.findByWatchlistIdAndSymbol(watchlist.getId(), normalized)
                .orElseThrow(() -> new ResourceNotFoundException(normalized + " is not in this watchlist"));
        watchlist.getStocks().remove(stock);
        watchlistStockRepository.delete(stock);
    }

    public List<String> symbolsForUser(Long userId) {
        return watchlistStockRepository.findByWatchlistUserId(userId).stream()
                .map(WatchlistStock::getSymbol)
                .distinct()
                .toList();
    }

    private Watchlist requireOwned(Long userId, Long watchlistId) {
        Watchlist watchlist = watchlistRepository.findById(watchlistId)
                .orElseThrow(() -> new ResourceNotFoundException("Watchlist not found"));
        if (!watchlist.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You do not have access to this watchlist");
        }
        return watchlist;
    }

    private String normalizeSymbol(String symbol) {
        if (symbol == null || !symbol.matches("(?i)^[A-Z.]{1,10}$")) {
            throw new InvalidSymbolException(symbol);
        }
        return symbol.toUpperCase(Locale.ROOT);
    }

    private WatchlistResponse toSummary(Watchlist watchlist) {
        return new WatchlistResponse(
                watchlist.getId(),
                watchlist.getName(),
                watchlist.getCreatedAt(),
                watchlist.getUpdatedAt(),
                watchlist.getStocks().size(),
                List.of()
        );
    }

    private WatchlistResponse toDetail(Watchlist watchlist) {
        List<String> symbols = watchlist.getStocks().stream().map(WatchlistStock::getSymbol).toList();
        Map<String, MarketQuoteDto> quotes = marketDataService.getQuotes(symbols);
        List<WatchlistStockResponse> stocks = watchlist.getStocks().stream()
                .map(stock -> new WatchlistStockResponse(stock.getSymbol(), stock.getAddedAt(), quotes.get(stock.getSymbol())))
                .toList();
        return new WatchlistResponse(
                watchlist.getId(),
                watchlist.getName(),
                watchlist.getCreatedAt(),
                watchlist.getUpdatedAt(),
                stocks.size(),
                stocks
        );
    }
}

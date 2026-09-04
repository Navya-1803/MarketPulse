import { useCallback, useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { StatusPill } from "../../components/market/StatusPill";
import { AddToWatchlistModal } from "../../components/watchlist/AddToWatchlistModal";
import { getErrorMessage } from "../../services/apiClient";
import { marketService } from "../../services/marketService";
import type { MarketQuote } from "../../types";
import { formatPercent, formatPrice, formatVolume, formatWhen } from "../../utils/format";

const PAGE_SIZE = 25;

const FILTERS = [
  { id: "ALL", label: "All Stocks" },
  { id: "GAINERS", label: "Gainers ▲" },
  { id: "LOSERS", label: "Losers ▼" },
  { id: "HIGH_VOLUME", label: "High Volume" },
  { id: "NEAR_HIGH", label: "Near Day High" },
  { id: "NEAR_LOW", label: "Near Day Low" },
] as const;

export function StocksPage() {
  const [stocks, setStocks] = useState<MarketQuote[]>([]);
  const [page, setPage] = useState(0);
  const [hasNext, setHasNext] = useState(false);
  const [initialLoading, setInitialLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState("");

  const [searchInput, setSearchInput] = useState("");
  const [debouncedQuery, setDebouncedQuery] = useState("");

  const [filter, setFilter] = useState<string>("ALL");
  const [sortKey, setSortKey] = useState<string>("symbol-asc");

  const [modalStock, setModalStock] = useState<MarketQuote | null>(null);
  const [successMsg, setSuccessMsg] = useState("");

  const observerTarget = useRef<HTMLDivElement>(null);
  const isFetchingRef = useRef(false);

  // Parse sortBy and sortDirection from sortKey
  const [sortBy, sortDirection] = sortKey.split("-");

  // Debounce search query input by 350ms
  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedQuery(searchInput.trim());
    }, 350);
    return () => clearTimeout(handler);
  }, [searchInput]);

  // Fetch stocks function
  const fetchPage = useCallback(
    async (pageToFetch: number, query: string, activeFilter: string, activeSortBy: string, activeSortDir: string, isInitial: boolean) => {
      if (isFetchingRef.current) {
        return;
      }
      isFetchingRef.current = true;

      if (isInitial) {
        setInitialLoading(true);
      } else {
        setLoadingMore(true);
      }
      setError("");

      try {
        const response = await marketService.stocks({
          page: pageToFetch,
          size: PAGE_SIZE,
          query: query || undefined,
          filter: activeFilter,
          sortBy: activeSortBy,
          sortDirection: activeSortDir,
        });

        setStocks((prev) => {
          if (pageToFetch === 0) {
            return response.content;
          }
          // Append unique stocks to prevent duplicates
          const existing = new Set(prev.map((s) => s.symbol));
          const fresh = response.content.filter((s) => !existing.has(s.symbol));
          return [...prev, ...fresh];
        });

        setPage(response.page);
        setHasNext(response.hasNext);
      } catch (err) {
        setError(getErrorMessage(err, "Failed to load market data"));
      } finally {
        setInitialLoading(false);
        setLoadingMore(false);
        isFetchingRef.current = false;
      }
    },
    []
  );

  // Triggered when query, filter, or sort changes -> reset to page 0
  useEffect(() => {
    setPage(0);
    setHasNext(false);
    void fetchPage(0, debouncedQuery, filter, sortBy, sortDirection, true);
  }, [debouncedQuery, filter, sortBy, sortDirection, fetchPage]);

  // Infinite scroll intersection observer
  useEffect(() => {
    const target = observerTarget.current;
    if (!target) {
      return;
    }

    const observer = new IntersectionObserver(
      (entries) => {
        const first = entries[0];
        if (
          first.isIntersecting &&
          hasNext &&
          !initialLoading &&
          !loadingMore &&
          !isFetchingRef.current
        ) {
          void fetchPage(page + 1, debouncedQuery, filter, sortBy, sortDirection, false);
        }
      },
      { rootMargin: "250px" }
    );

    observer.observe(target);
    return () => {
      observer.disconnect();
    };
  }, [hasNext, initialLoading, loadingMore, page, debouncedQuery, filter, sortBy, sortDirection, fetchPage]);

  const handleAddedToWatchlist = (watchlistName: string) => {
    if (modalStock) {
      setSuccessMsg(`${modalStock.symbol} added to ${watchlistName}`);
      setTimeout(() => setSuccessMsg(""), 4000);
    }
  };

  return (
    <div className="stocks-page">
      <header className="page-head">
        <div>
          <p className="eyebrow">Explore Stocks</p>
          <h1>Market</h1>
          <p className="muted">
            Browse global stocks, view real market data, and add to your watchlists.
          </p>
        </div>
      </header>

      {successMsg ? <div className="alert ok-alert">{successMsg}</div> : null}
      {error ? <div className="alert">{error}</div> : null}

      {/* Search & Controls Section */}
      <div className="search-section">
        <div className="search-box">
          <input
            type="search"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            placeholder="Search stocks by symbol or company name"
            aria-label="Search stocks"
          />
          {searchInput ? (
            <button
              type="button"
              className="clear-search-btn"
              onClick={() => setSearchInput("")}
              aria-label="Clear search"
            >
              ✕
            </button>
          ) : null}
        </div>

        {/* Sort dropdown */}
        <div className="sort-control">
          <label htmlFor="stock-sort-select" className="muted" style={{ fontSize: "0.8rem", marginRight: "0.5rem" }}>
            Sort:
          </label>
          <select
            id="stock-sort-select"
            value={sortKey}
            onChange={(e) => setSortKey(e.target.value)}
            className="select-input"
            aria-label="Sort stocks"
          >
            <option value="symbol-asc">Symbol (A → Z)</option>
            <option value="symbol-desc">Symbol (Z → A)</option>
            <option value="name-asc">Company Name (A → Z)</option>
            <option value="changepercent-desc">% Change (High → Low)</option>
            <option value="changepercent-asc">% Change (Low → High)</option>
            <option value="price-desc">Price (High → Low)</option>
            <option value="price-asc">Price (Low → High)</option>
            <option value="volume-desc">Volume (High → Low)</option>
          </select>
        </div>
      </div>

      {/* Filter Tabs */}
      <div className="filter-tabs-bar" role="tablist" aria-label="Stock filters">
        {FILTERS.map((f) => (
          <button
            key={f.id}
            type="button"
            role="tab"
            aria-selected={filter === f.id}
            className={`filter-tab-btn ${filter === f.id ? "active" : ""}`}
            onClick={() => setFilter(f.id)}
          >
            {f.label}
          </button>
        ))}
      </div>

      {initialLoading ? (
        <div className="scroll-status-container">
          <p className="muted">Loading market data...</p>
        </div>
      ) : stocks.length === 0 ? (
        <div className="empty-card">
          {debouncedQuery ? (
            <>
              <h3>No stocks found matching "{debouncedQuery}"</h3>
              <p className="muted">
                Try searching by symbol (e.g. NVDA, AAPL) or company name (e.g. Tesla, Microsoft).
              </p>
            </>
          ) : (
            <p className="muted">No stocks available under this filter.</p>
          )}
        </div>
      ) : (
        <>
          <div className="stocks-grid">
            {stocks.map((stock) => {
              const isUp = Number(stock.changePercent) >= 0;
              const changeSign = isUp ? "+" : "";
              const isUnavailable = stock.status === "UNAVAILABLE";

              return (
                <article key={stock.symbol} className="stock-card">
                  <div className="stock-card-header">
                    <div>
                      <h3 className="stock-symbol">{stock.symbol}</h3>
                      <p className="stock-company">{stock.companyName || stock.symbol}</p>
                    </div>
                    <StatusPill status={stock.status} />
                  </div>

                  {isUnavailable ? (
                    <div className="unavailable-notice">
                      <p className="down">Market data temporarily unavailable</p>
                    </div>
                  ) : (
                    <>
                      <div className="stock-price-block">
                        <span className="stock-current-price">{formatPrice(stock.price)}</span>
                        <span className={`stock-price-change ${isUp ? "up" : "down"}`}>
                          {changeSign}
                          {stock.changeAmount != null
                            ? `$${Math.abs(stock.changeAmount).toFixed(2)} `
                            : ""}
                          ({formatPercent(stock.changePercent)})
                        </span>
                      </div>

                      <div className="stock-stats-grid-2x2">
                        <div className="stock-stat">
                          <span className="stat-label">High</span>
                          <span className="stat-val">{formatPrice(stock.dayHigh)}</span>
                        </div>
                        <div className="stock-stat">
                          <span className="stat-label">Low</span>
                          <span className="stat-val">{formatPrice(stock.dayLow)}</span>
                        </div>
                        <div className="stock-stat">
                          <span className="stat-label">Open</span>
                          <span className="stat-val">{formatPrice(stock.open)}</span>
                        </div>
                        <div className="stock-stat">
                          <span className="stat-label">Prev Cls</span>
                          <span className="stat-val">{formatPrice(stock.previousClose)}</span>
                        </div>
                      </div>

                      <div className="stock-meta-tags">
                        {stock.volume != null ? (
                          <span className="meta-tag">Vol: {formatVolume(stock.volume)}</span>
                        ) : null}
                        {stock.exchange ? (
                          <span className="meta-tag">{stock.exchange}</span>
                        ) : null}
                        {stock.currency ? (
                          <span className="meta-tag">{stock.currency}</span>
                        ) : null}
                      </div>

                      <div className="stock-footer-meta">
                        <span className="muted">Updated: {formatWhen(stock.capturedAt)}</span>
                      </div>
                    </>
                  )}

                  <div className="stock-card-actions">
                    <Link to={`/stocks/${stock.symbol}`} className="ghost-btn action-link">
                      View Details
                    </Link>
                    <button
                      type="button"
                      className="action-btn"
                      onClick={() => setModalStock(stock)}
                    >
                      + Add to Watchlist
                    </button>
                  </div>
                </article>
              );
            })}
          </div>

          {/* Infinite scroll sentinel and state indicators */}
          <div ref={observerTarget} className="infinite-scroll-sentinel" />

          <div className="scroll-status-container">
            {loadingMore ? (
              <p className="muted">Loading more stocks...</p>
            ) : !hasNext && stocks.length > 0 ? (
              <p className="muted">No more stocks to load.</p>
            ) : null}
          </div>
        </>
      )}

      <AddToWatchlistModal
        symbol={modalStock?.symbol ?? ""}
        companyName={modalStock?.companyName}
        isOpen={Boolean(modalStock)}
        onClose={() => setModalStock(null)}
        onSuccess={handleAddedToWatchlist}
      />
    </div>
  );
}

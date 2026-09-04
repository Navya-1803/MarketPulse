import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { StatusPill } from "../../components/market/StatusPill";
import { AddToWatchlistModal } from "../../components/watchlist/AddToWatchlistModal";
import { getErrorMessage } from "../../services/apiClient";
import { marketService } from "../../services/marketService";
import type { MarketQuote } from "../../types";
import { formatPercent, formatPrice, formatVolume, formatWhen } from "../../utils/format";

export function StocksPage() {
  const [stocks, setStocks] = useState<MarketQuote[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [searchQuery, setSearchQuery] = useState("");
  const [modalStock, setModalStock] = useState<MarketQuote | null>(null);
  const [successMsg, setSuccessMsg] = useState("");

  const loadStocks = async () => {
    setLoading(true);
    setError("");
    try {
      const data = await marketService.stocks();
      setStocks(data);
    } catch (err) {
      setError(getErrorMessage(err, "Failed to load market stocks"));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadStocks();
  }, []);

  const filteredStocks = useMemo(() => {
    const q = searchQuery.trim().toLowerCase();
    if (!q) {
      return stocks;
    }
    return stocks.filter(
      (s) =>
        s.symbol.toLowerCase().includes(q) ||
        (s.companyName && s.companyName.toLowerCase().includes(q))
    );
  }, [stocks, searchQuery]);

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
          <p className="muted">Browse available stocks, view real-time market data, and add to your watchlists.</p>
        </div>
      </header>

      {successMsg ? <div className="alert ok-alert">{successMsg}</div> : null}
      {error ? <div className="alert">{error}</div> : null}

      <div className="search-section">
        <div className="search-box">
          <input
            type="search"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search stocks by symbol or company name"
            aria-label="Search stocks"
          />
          {searchQuery ? (
            <button
              type="button"
              className="clear-search-btn"
              onClick={() => setSearchQuery("")}
              aria-label="Clear search"
            >
              ✕
            </button>
          ) : null}
        </div>
      </div>

      {loading ? (
        <p className="muted">Loading available market stocks…</p>
      ) : filteredStocks.length === 0 ? (
        <div className="empty-card">
          {searchQuery ? (
            <>
              <h3>No stocks found matching "{searchQuery}"</h3>
              <p className="muted">Try searching by symbol (e.g. NVDA, AAPL) or company name (e.g. Tesla, Microsoft).</p>
            </>
          ) : (
            <p className="muted">No stocks available at this time.</p>
          )}
        </div>
      ) : (
        <div className="stocks-grid">
          {filteredStocks.map((stock) => {
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
                        {stock.changeAmount != null ? `$${Math.abs(stock.changeAmount).toFixed(2)} ` : ""}
                        ({formatPercent(stock.changePercent)})
                      </span>
                    </div>

                    <div className="stock-stats-row">
                      <div className="stock-stat">
                        <span className="stat-label">High</span>
                        <span className="stat-val">{formatPrice(stock.dayHigh)}</span>
                      </div>
                      <div className="stock-stat">
                        <span className="stat-label">Low</span>
                        <span className="stat-val">{formatPrice(stock.dayLow)}</span>
                      </div>
                      <div className="stock-stat">
                        <span className="stat-label">Volume</span>
                        <span className="stat-val">{formatVolume(stock.volume)}</span>
                      </div>
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

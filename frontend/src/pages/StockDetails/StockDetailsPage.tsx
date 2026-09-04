import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { SeverityBadge } from "../../components/market/SeverityBadge";
import { StatusPill } from "../../components/market/StatusPill";
import { StockPriceChart } from "../../components/market/StockPriceChart";
import { AddToWatchlistModal } from "../../components/watchlist/AddToWatchlistModal";
import { getErrorMessage } from "../../services/apiClient";
import { marketService } from "../../services/marketService";
import type { AttentionItem, MarketQuote } from "../../types";
import { formatPercent, formatPrice, formatVolume, formatWhen } from "../../utils/format";

export function StockDetailsPage() {
  const { symbol = "" } = useParams();
  const [quote, setQuote] = useState<MarketQuote | null>(null);
  const [change, setChange] = useState<AttentionItem | null>(null);
  const [error, setError] = useState("");
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [successMsg, setSuccessMsg] = useState("");

  useEffect(() => {
    void Promise.all([marketService.quote(symbol), marketService.changes()])
      .then(([q, summary]) => {
        setQuote(q);
        setChange(summary.items.find((item) => item.symbol === symbol.toUpperCase()) ?? null);
      })
      .catch((err) => setError(getErrorMessage(err, "Could not load quote")));
  }, [symbol]);

  const handleAddedToWatchlist = (watchlistName: string) => {
    setSuccessMsg(`${symbol.toUpperCase()} added to ${watchlistName}`);
    setTimeout(() => setSuccessMsg(""), 4000);
  };

  if (error) {
    return <div className="alert">{error}</div>;
  }
  if (!quote) {
    return <p className="muted">Loading {symbol}…</p>;
  }

  const isUp = Number(quote.changePercent) >= 0;
  const changeSign = isUp ? "+" : "";

  // Calculate position on day range
  let dayRangePercent = 50;
  if (quote.dayHigh && quote.dayLow && quote.price && quote.dayHigh > quote.dayLow) {
    dayRangePercent = Math.max(
      0,
      Math.min(100, ((quote.price - quote.dayLow) / (quote.dayHigh - quote.dayLow)) * 100)
    );
  }

  return (
    <div className="stock-details-page">
      <p style={{ display: "flex", gap: "1rem" }}>
        <Link to="/stocks" className="text-link">
          ← All Stocks
        </Link>
        <span className="muted">·</span>
        <Link to="/dashboard" className="text-link">
          Dashboard
        </Link>
      </p>

      {successMsg ? (
        <div className="alert ok-alert" style={{ marginBottom: "1rem" }}>
          {successMsg}
        </div>
      ) : null}

      <header className="page-head">
        <div>
          <p className="eyebrow">{quote.companyName || quote.symbol}</p>
          <h1>{quote.symbol}</h1>
        </div>
        <div style={{ display: "flex", alignItems: "center", gap: "0.8rem" }}>
          <StatusPill status={quote.status} />
          <button type="button" onClick={() => setIsModalOpen(true)}>
            + Add to Watchlist
          </button>
        </div>
      </header>

      {/* Hero Price Display */}
      <section className="hero-card">
        <p className="display-count">{formatPrice(quote.price)}</p>
        <p className={isUp ? "up" : "down"}>
          {changeSign}
          {quote.changeAmount != null ? `$${Math.abs(quote.changeAmount).toFixed(2)} ` : ""}
          ({formatPercent(quote.changePercent)})
        </p>
        {quote.message ? <p className="muted">{quote.message}</p> : null}
      </section>

      {/* Interactive Historical Price Chart */}
      <StockPriceChart symbol={quote.symbol} />

      {/* Today's Range visual bar */}
      <section className="range-bar-card">
        <div className="range-labels">
          <span>
            <small className="muted">Day Low</small>
            <br />
            <strong>{formatPrice(quote.dayLow)}</strong>
          </span>
          <span className="muted">Today's Trading Range</span>
          <span style={{ textAlign: "right" }}>
            <small className="muted">Day High</small>
            <br />
            <strong>{formatPrice(quote.dayHigh)}</strong>
          </span>
        </div>
        <div className="range-track">
          <div className="range-indicator" style={{ left: `${dayRangePercent}%` }} />
        </div>
      </section>

      {/* Market Information Grid */}
      <div className="stat-grid" style={{ marginTop: "1.2rem" }}>
        <article>
          <span>Open</span>
          <strong>{formatPrice(quote.open)}</strong>
        </article>
        <article>
          <span>Previous Close</span>
          <strong>{formatPrice(quote.previousClose)}</strong>
        </article>
        <article>
          <span>Day High</span>
          <strong>{formatPrice(quote.dayHigh)}</strong>
        </article>
        <article>
          <span>Day Low</span>
          <strong>{formatPrice(quote.dayLow)}</strong>
        </article>
        <article>
          <span>Volume</span>
          <strong>{formatVolume(quote.volume)}</strong>
        </article>
        <article>
          <span>Exchange</span>
          <strong>{quote.exchange || "US"}</strong>
        </article>
        <article>
          <span>Currency</span>
          <strong>{quote.currency || "USD"}</strong>
        </article>
        <article>
          <span>Last Updated</span>
          <strong>{formatWhen(quote.capturedAt)}</strong>
        </article>
      </div>

      {/* MarketPulse Intelligence Insight */}
      {change ? (
        <section className="block attention-insight-block" style={{ marginTop: "1.5rem" }}>
          <div className="insight-header">
            <div>
              <p className="eyebrow">MarketPulse Intelligence</p>
              <h2>Movement Since Your Last Check</h2>
            </div>
            <div style={{ display: "flex", alignItems: "center", gap: "0.8rem" }}>
              <SeverityBadge severity={change.severity} />
              <span className="mono attention-score-badge">
                Attention Score: {change.attentionScore}
              </span>
            </div>
          </div>

          <p className={`move ${change.changePercent >= 0 ? "up" : "down"}`} style={{ fontSize: "1.3rem", fontWeight: 700, margin: "0.6rem 0" }}>
            {change.changePercent >= 0 ? "↑" : "↓"} {formatPercent(change.changePercent)}
            <span className="muted" style={{ fontSize: "0.9rem", fontWeight: 400, marginLeft: "0.8rem" }}>
              (from {formatPrice(change.previousPrice)} to {formatPrice(change.currentPrice)})
            </span>
          </p>

          <h4>Why this matters:</h4>
          <ul className="reasons-list">
            {change.reasons.map((reason) => (
              <li key={reason}>{reason}</li>
            ))}
          </ul>
        </section>
      ) : null}

      <AddToWatchlistModal
        symbol={quote.symbol}
        companyName={quote.companyName}
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onSuccess={handleAddedToWatchlist}
      />
    </div>
  );
}

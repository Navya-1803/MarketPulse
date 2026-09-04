import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { StatusPill } from "../../components/market/StatusPill";
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

  return (
    <div>
      <p style={{ display: "flex", gap: "1rem" }}>
        <Link to="/stocks" className="text-link">
          ← All Stocks
        </Link>
        <span className="muted">·</span>
        <Link to="/dashboard" className="text-link">
          Dashboard
        </Link>
      </p>

      {successMsg ? <div className="alert ok-alert" style={{ marginBottom: "1rem" }}>{successMsg}</div> : null}

      <header className="page-head">
        <div>
          <p className="eyebrow">{quote.companyName}</p>
          <h1>{quote.symbol}</h1>
        </div>
        <div style={{ display: "flex", alignItems: "center", gap: "0.8rem" }}>
          <StatusPill status={quote.status} />
          <button type="button" onClick={() => setIsModalOpen(true)}>
            + Add to Watchlist
          </button>
        </div>
      </header>
      <section className="hero-card">
        <p className="display-count">{formatPrice(quote.price)}</p>
        <p className={Number(quote.changePercent) >= 0 ? "up" : "down"}>{formatPercent(quote.changePercent)}</p>
        {quote.message ? <p className="muted">{quote.message}</p> : null}
      </section>
      <div className="stat-grid">
        <article>
          <span>Day high</span>
          <strong>{formatPrice(quote.dayHigh)}</strong>
        </article>
        <article>
          <span>Day low</span>
          <strong>{formatPrice(quote.dayLow)}</strong>
        </article>
        <article>
          <span>Volume</span>
          <strong>{formatVolume(quote.volume)}</strong>
        </article>
        <article>
          <span>Last updated</span>
          <strong>{formatWhen(quote.capturedAt)}</strong>
        </article>
      </div>
      {change ? (
        <section className="block">
          <h2>Since your last check</h2>
          <p className={`move ${change.changePercent >= 0 ? "up" : "down"}`}>
            {change.changePercent >= 0 ? "↑" : "↓"} {formatPercent(change.changePercent)}
          </p>
          <ul>
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

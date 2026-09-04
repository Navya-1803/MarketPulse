import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { StatusPill } from "../../components/market/StatusPill";
import { getErrorMessage } from "../../services/apiClient";
import { marketService } from "../../services/marketService";
import type { AttentionItem, MarketQuote } from "../../types";
import { formatPercent, formatPrice, formatVolume, formatWhen } from "../../utils/format";

export function StockDetailsPage() {
  const { symbol = "" } = useParams();
  const [quote, setQuote] = useState<MarketQuote | null>(null);
  const [change, setChange] = useState<AttentionItem | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    void Promise.all([marketService.quote(symbol), marketService.changes()])
      .then(([q, summary]) => {
        setQuote(q);
        setChange(summary.items.find((item) => item.symbol === symbol.toUpperCase()) ?? null);
      })
      .catch((err) => setError(getErrorMessage(err, "Could not load quote")));
  }, [symbol]);

  if (error) {
    return <div className="alert">{error}</div>;
  }
  if (!quote) {
    return <p className="muted">Loading {symbol}…</p>;
  }

  return (
    <div>
      <p>
        <Link to="/dashboard" className="text-link">
          ← Dashboard
        </Link>
      </p>
      <header className="page-head">
        <div>
          <p className="eyebrow">{quote.companyName}</p>
          <h1>{quote.symbol}</h1>
        </div>
        <StatusPill status={quote.status} />
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
    </div>
  );
}

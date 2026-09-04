import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { SeverityBadge } from "../../components/market/SeverityBadge";
import { useAuth } from "../../features/auth/AuthContext";
import { getErrorMessage } from "../../services/apiClient";
import { marketService } from "../../services/marketService";
import type { DashboardData } from "../../types";
import { formatPercent, formatWhen, greeting } from "../../utils/format";

export function DashboardPage() {
  const { user } = useAuth();
  const [data, setData] = useState<DashboardData | null>(null);
  const [error, setError] = useState("");
  const [ackMessage, setAckMessage] = useState("");
  const [loading, setLoading] = useState(true);

  const load = async () => {
    setLoading(true);
    try {
      setData(await marketService.dashboard());
    } catch (err) {
      setError(getErrorMessage(err, "Could not load dashboard"));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, []);

  const acknowledge = async () => {
    try {
      await marketService.acknowledge();
      setAckMessage("Last checked time updated.");
      await load();
    } catch (err) {
      setError(getErrorMessage(err, "Could not update checkpoint"));
    }
  };

  if (loading) {
    return <p className="muted">Loading your attention feed…</p>;
  }
  if (error) {
    return <div className="alert">{error}</div>;
  }
  if (!data) {
    return null;
  }

  const meaningful = data.changes.items.filter((item) => item.severity !== "NORMAL");

  return (
    <div>
      <header className="page-head">
        <div>
          <p className="eyebrow">Dashboard</p>
          <h1>{greeting(data.user.name || user?.name)}</h1>
          <p className="muted">Last checked: {formatWhen(data.changes.lastCheckedAt)}</p>
        </div>
        <button type="button" onClick={() => void acknowledge()}>
          Mark as checked
        </button>
      </header>
      {ackMessage ? <p className="ok">{ackMessage}</p> : null}

      <section className="hero-card">
        <h2>What changed?</h2>
        <p className="display-count">{data.changes.meaningfulChangeCount} meaningful changes</p>
        <p className="muted">Ranked by attention score. Checkpoint is not updated until you mark this as checked.</p>
      </section>

      <div className="stack">
        {meaningful.length === 0 ? (
          <div className="empty-card">No significant moves since your last check.</div>
        ) : (
          meaningful.map((item) => (
            <Link key={item.symbol} to={`/stocks/${item.symbol}`} className="attention-card">
              <div className="attention-top">
                <SeverityBadge severity={item.severity} />
                <span className="mono">Score {item.attentionScore}</span>
              </div>
              <h3>{item.symbol}</h3>
              <p className="company">{item.companyName}</p>
              <p className={`move ${item.changePercent >= 0 ? "up" : "down"}`}>
                {item.changePercent >= 0 ? "↑" : "↓"} {formatPercent(item.changePercent)}
              </p>
              <ul>
                {item.reasons.map((reason) => (
                  <li key={reason}>{reason}</li>
                ))}
              </ul>
            </Link>
          ))
        )}
      </div>

      <section className="block">
        <div className="page-head">
          <h2>Your watchlists</h2>
          <Link to="/watchlists" className="text-link">
            Manage
          </Link>
        </div>
        <div className="card-grid">
          {data.watchlists.length === 0 ? (
            <div className="empty-card">Create a watchlist to start tracking symbols.</div>
          ) : (
            data.watchlists.map((list) => (
              <Link key={list.id} to={`/watchlists/${list.id}`} className="mini-card">
                <h3>{list.name}</h3>
                <p>{list.stockCount} stocks</p>
              </Link>
            ))
          )}
        </div>
      </section>
    </div>
  );
}

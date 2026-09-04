import { useEffect, useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import { getErrorMessage } from "../../services/apiClient";
import { watchlistService } from "../../services/watchlistService";
import type { Watchlist } from "../../types";

interface AddToWatchlistModalProps {
  symbol: string;
  companyName?: string | null;
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (watchlistName: string) => void;
}

export function AddToWatchlistModal({
  symbol,
  companyName,
  isOpen,
  onClose,
  onSuccess,
}: AddToWatchlistModalProps) {
  const [watchlists, setWatchlists] = useState<Watchlist[]>([]);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!isOpen) {
      setSelectedId(null);
      setError("");
      return;
    }
    setLoading(true);
    setError("");
    watchlistService
      .list()
      .then((lists) => {
        setWatchlists(lists);
        if (lists.length > 0) {
          setSelectedId(lists[0].id);
        }
      })
      .catch((err) => setError(getErrorMessage(err, "Failed to load watchlists")))
      .finally(() => setLoading(false));
  }, [isOpen]);

  if (!isOpen) {
    return null;
  }

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!selectedId) {
      setError("Please select a watchlist");
      return;
    }
    setSubmitting(true);
    setError("");
    try {
      const selected = watchlists.find((w) => w.id === selectedId);
      await watchlistService.addStock(selectedId, symbol);
      onSuccess(selected ? selected.name : "Watchlist");
      onClose();
    } catch (err) {
      setError(getErrorMessage(err, `Failed to add ${symbol} to watchlist`));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-card" onClick={(e) => e.stopPropagation()}>
        <header className="modal-head">
          <div>
            <p className="eyebrow">Add to Watchlist</p>
            <h2>{symbol}</h2>
            {companyName ? <p className="muted">{companyName}</p> : null}
          </div>
          <button type="button" className="close-btn" onClick={onClose} aria-label="Close">
            ✕
          </button>
        </header>

        {error ? <div className="alert">{error}</div> : null}

        {loading ? (
          <p className="muted">Loading your watchlists…</p>
        ) : watchlists.length === 0 ? (
          <div className="empty-state-modal">
            <p className="muted">You don't have any watchlists yet.</p>
            <Link to="/watchlists" className="text-link" onClick={onClose}>
              + Create your first watchlist
            </Link>
          </div>
        ) : (
          <form onSubmit={(e) => void handleSubmit(e)}>
            <p className="field-label">Select destination watchlist:</p>
            <div className="watchlist-radio-group">
              {watchlists.map((wl) => (
                <label key={wl.id} className={`watchlist-radio-option ${selectedId === wl.id ? "selected" : ""}`}>
                  <input
                    type="radio"
                    name="watchlist"
                    value={wl.id}
                    checked={selectedId === wl.id}
                    onChange={() => setSelectedId(wl.id)}
                  />
                  <div className="option-info">
                    <strong>{wl.name}</strong>
                    <span>{wl.stockCount} stocks</span>
                  </div>
                </label>
              ))}
            </div>

            <div className="modal-actions">
              <button type="button" className="ghost-btn" onClick={onClose} disabled={submitting}>
                Cancel
              </button>
              <button type="submit" disabled={submitting || !selectedId}>
                {submitting ? "Adding…" : "Add to Watchlist"}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}

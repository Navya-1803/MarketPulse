import { useEffect, useState, type FormEvent } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { StatusPill } from "../../components/market/StatusPill";
import { getErrorMessage } from "../../services/apiClient";
import { watchlistService } from "../../services/watchlistService";
import type { Watchlist } from "../../types";
import { formatPercent, formatPrice } from "../../utils/format";

export function WatchlistPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [lists, setLists] = useState<Watchlist[]>([]);
  const [detail, setDetail] = useState<Watchlist | null>(null);
  const [name, setName] = useState("");
  const [rename, setRename] = useState("");
  const [symbol, setSymbol] = useState("");
  const [error, setError] = useState("");

  const loadLists = async () => {
    setLists(await watchlistService.list());
  };

  useEffect(() => {
    void loadLists().catch((err) => setError(getErrorMessage(err)));
  }, []);

  useEffect(() => {
    if (!id) {
      setDetail(null);
      return;
    }
    void watchlistService
      .get(Number(id))
      .then((list) => {
        setDetail(list);
        setRename(list.name);
      })
      .catch((err) => setError(getErrorMessage(err)));
  }, [id]);

  const createList = async (event: FormEvent) => {
    event.preventDefault();
    try {
      const created = await watchlistService.create(name);
      setName("");
      await loadLists();
      navigate(`/watchlists/${created.id}`);
    } catch (err) {
      setError(getErrorMessage(err, "Could not create watchlist"));
    }
  };

  const renameList = async (event: FormEvent) => {
    event.preventDefault();
    if (!detail) {
      return;
    }
    try {
      const updated = await watchlistService.rename(detail.id, rename);
      setDetail(updated);
      await loadLists();
    } catch (err) {
      setError(getErrorMessage(err, "Could not rename watchlist"));
    }
  };

  const addStock = async (event: FormEvent) => {
    event.preventDefault();
    if (!detail) {
      return;
    }
    try {
      const updated = await watchlistService.addStock(detail.id, symbol);
      setSymbol("");
      setDetail(updated);
      await loadLists();
    } catch (err) {
      setError(getErrorMessage(err, "Could not add symbol"));
    }
  };

  const removeStock = async (ticker: string) => {
    if (!detail) {
      return;
    }
    await watchlistService.removeStock(detail.id, ticker);
    setDetail(await watchlistService.get(detail.id));
  };

  const removeList = async () => {
    if (!detail) {
      return;
    }
    await watchlistService.remove(detail.id);
    navigate("/watchlists");
    await loadLists();
    setDetail(null);
  };

  return (
    <div className="split">
      <section>
        <header className="page-head">
          <h1>Watchlists</h1>
        </header>
        {error ? <div className="alert">{error}</div> : null}
        <form className="inline-form" onSubmit={(e) => void createList(e)}>
          <input value={name} onChange={(e) => setName(e.target.value)} placeholder="My Tech Stocks" required />
          <button type="submit">Create</button>
        </form>
        <div className="stack">
          {lists.map((list) => (
            <Link key={list.id} to={`/watchlists/${list.id}`} className={`mini-card ${id === String(list.id) ? "active" : ""}`}>
              <h3>{list.name}</h3>
              <p>{list.stockCount} stocks</p>
            </Link>
          ))}
        </div>
      </section>
      <section>
        {detail ? (
          <>
            <header className="page-head">
              <div>
                <p className="eyebrow">Watchlist</p>
                <h1>{detail.name}</h1>
              </div>
              <button type="button" className="ghost-btn" onClick={() => void removeList()}>
                Delete
              </button>
            </header>
            <form className="inline-form" onSubmit={(e) => void renameList(e)}>
              <input value={rename} onChange={(e) => setRename(e.target.value)} required />
              <button type="submit" className="ghost-btn">
                Rename
              </button>
            </form>
            <form className="inline-form" onSubmit={(e) => void addStock(e)}>
              <input
                value={symbol}
                onChange={(e) => setSymbol(e.target.value.toUpperCase())}
                placeholder="Add stock e.g. NVDA"
                required
              />
              <button type="submit">+ Add stock</button>
            </form>
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Symbol</th>
                    <th>Price</th>
                    <th>Change</th>
                    <th>Status</th>
                    <th />
                  </tr>
                </thead>
                <tbody>
                  {detail.stocks.map((stock) => (
                    <tr key={stock.symbol}>
                      <td>
                        <Link to={`/stocks/${stock.symbol}`}>{stock.symbol}</Link>
                      </td>
                      <td className="mono">{formatPrice(stock.quote?.price)}</td>
                      <td className={Number(stock.quote?.changePercent) >= 0 ? "up" : "down"}>
                        {formatPercent(stock.quote?.changePercent)}
                      </td>
                      <td>{stock.quote ? <StatusPill status={stock.quote.status} /> : "—"}</td>
                      <td>
                        <button type="button" className="ghost-btn" onClick={() => void removeStock(stock.symbol)}>
                          Remove
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </>
        ) : (
          <div className="empty-card">Select or create a watchlist.</div>
        )}
      </section>
    </div>
  );
}

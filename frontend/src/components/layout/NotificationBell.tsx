import { useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { marketService } from "../../services/marketService";
import type { NotificationItem } from "../../types";
import { formatPercent, formatWhen } from "../../utils/format";

export function NotificationBell() {
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  const loadNotifications = () => {
    marketService
      .notifications()
      .then((data) => setNotifications(data || []))
      .catch(() => {});
  };

  useEffect(() => {
    loadNotifications();
    const interval = setInterval(loadNotifications, 30000);
    return () => clearInterval(interval);
  }, []);

  // Close dropdown on outside click
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const handleAcknowledgeAll = async () => {
    try {
      await marketService.acknowledgeAllNotifications();
      setNotifications([]);
    } catch {}
  };

  const handleAcknowledgeSingle = async (id: number) => {
    try {
      await marketService.acknowledgeNotification(id);
      setNotifications((prev) => prev.filter((n) => n.id !== id));
    } catch {}
  };

  const count = notifications.length;

  return (
    <div className="notification-bell-container" ref={containerRef}>
      <button
        type="button"
        className="notification-bell-btn"
        onClick={() => setIsOpen((prev) => !prev)}
        aria-label="Notifications"
        title="Meaningful change notifications"
      >
        <span className="bell-icon">🔔</span>
        {count > 0 ? <span className="notification-badge">{count > 9 ? "9+" : count}</span> : null}
      </button>

      {isOpen ? (
        <div className="notification-dropdown">
          <div className="notification-dropdown-header">
            <h4>Meaningful Change Alerts</h4>
            {count > 0 ? (
              <button
                type="button"
                className="ghost-btn"
                style={{ fontSize: "0.75rem", padding: "0.2rem 0.5rem" }}
                onClick={handleAcknowledgeAll}
              >
                Clear all
              </button>
            ) : null}
          </div>

          <div className="notification-list">
            {count === 0 ? (
              <p className="muted" style={{ padding: "1.2rem", textAlign: "center", fontSize: "0.85rem" }}>
                No active meaningful change alerts.
              </p>
            ) : (
              notifications.map((item) => {
                const isUp = item.changePercent >= 0;
                return (
                  <div key={item.id} className="notification-item">
                    <div className="notification-item-header">
                      <Link
                        to={`/stocks/${item.symbol}`}
                        className="text-link"
                        onClick={() => setIsOpen(false)}
                        style={{ fontWeight: 700 }}
                      >
                        {item.symbol}
                      </Link>
                      <span className={`move ${isUp ? "up" : "down"}`} style={{ fontSize: "0.85rem" }}>
                        {isUp ? "↑" : "↓"} {formatPercent(item.changePercent)}
                      </span>
                      <button
                        type="button"
                        className="close-btn"
                        style={{ padding: "0 0.3rem", fontSize: "0.8rem" }}
                        onClick={() => handleAcknowledgeSingle(item.id)}
                        title="Dismiss alert"
                      >
                        ✕
                      </button>
                    </div>
                    <p className="notification-reason" style={{ fontSize: "0.8rem", margin: "0.25rem 0" }}>
                      {item.reasons && item.reasons.length ? item.reasons[0] : "Meaningful move detected"}
                    </p>
                    <span className="muted" style={{ fontSize: "0.72rem" }}>
                      {formatWhen(item.detectedAt)} · Score {item.attentionScore}
                    </span>
                  </div>
                );
              })
            )}
          </div>
        </div>
      ) : null}
    </div>
  );
}

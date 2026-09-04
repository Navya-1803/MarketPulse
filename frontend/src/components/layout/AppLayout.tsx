import { Link, NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../../features/auth/AuthContext";
import { NotificationBell } from "./NotificationBell";

export function AppLayout() {
  const { user, logout } = useAuth();

  return (
    <div className="app-shell">
      <aside className="side-nav">
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
          <Link to="/dashboard" className="brand">
            <span className="brand-mark" />
            MarketPulse
          </Link>
          <NotificationBell />
        </div>
        <p className="brand-tag">Track · Remember · Compare</p>
        <nav>
          <NavLink to="/dashboard">Dashboard</NavLink>
          <NavLink to="/watchlists">Watchlists</NavLink>
          <NavLink to="/stocks">Stocks</NavLink>
          <NavLink to="/profile">Profile / Settings</NavLink>
        </nav>
        <div className="side-footer">
          <div className="user-chip">
            <strong>{user?.name}</strong>
            <span>{user?.email}</span>
          </div>
          <button type="button" className="ghost-btn" onClick={() => void logout()}>
            Log out
          </button>
        </div>
      </aside>
      <main className="app-main">
        <Outlet />
      </main>
    </div>
  );
}

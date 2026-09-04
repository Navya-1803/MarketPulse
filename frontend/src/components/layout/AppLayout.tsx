import { Link, NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../../features/auth/AuthContext";

export function AppLayout() {
  const { user, logout } = useAuth();

  return (
    <div className="app-shell">
      <aside className="side-nav">
        <Link to="/dashboard" className="brand">
          <span className="brand-mark" />
          MarketPulse
        </Link>
        <p className="brand-tag">Track · Remember · Compare</p>
        <nav>
          <NavLink to="/dashboard">Dashboard</NavLink>
          <NavLink to="/watchlists">Watchlists</NavLink>
          <NavLink to="/stocks">Stocks</NavLink>
          <NavLink to="/profile">Profile</NavLink>
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

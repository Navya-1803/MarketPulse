import { useState, type FormEvent } from "react";
import { Link, Navigate, useNavigate } from "react-router-dom";
import { useAuth } from "../../features/auth/AuthContext";
import { getErrorMessage } from "../../services/apiClient";

export function LoginPage() {
  const { isAuthenticated, login } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  if (isAuthenticated) {
    return <Navigate to="/dashboard" replace />;
  }

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setLoading(true);
    setError("");
    try {
      await login(email, password);
      navigate("/dashboard");
    } catch (err) {
      setError(getErrorMessage(err, "Unable to log in"));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-screen">
      <div className="auth-panel">
        <div className="auth-copy">
          <p className="eyebrow">MarketPulse</p>
          <h1>See what actually moved since you last looked.</h1>
          <p>
            A watchlist that remembers your last check, compares the current market, and ranks the
            changes that deserve attention.
          </p>
        </div>
        <form className="auth-form" onSubmit={(e) => void onSubmit(e)}>
          <h2>Log in</h2>
          {error ? <div className="alert">{error}</div> : null}
          <label>
            Email
            <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
          </label>
          <label>
            Password
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
          </label>
          <button type="submit" disabled={loading}>
            {loading ? "Signing in…" : "Login"}
          </button>
          <p className="muted">
            Don't have an account? <Link to="/register">Register</Link>
          </p>
        </form>
      </div>
    </div>
  );
}

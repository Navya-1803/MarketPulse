import { useEffect, useState } from "react";
import { useAuth } from "../../features/auth/AuthContext";
import { getErrorMessage } from "../../services/apiClient";
import { marketService } from "../../services/marketService";

export function ProfilePage() {
  const { user } = useAuth();
  const [threshold, setThreshold] = useState<number>(3.0);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [savedMsg, setSavedMsg] = useState("");
  const [errorMsg, setErrorMsg] = useState("");

  useEffect(() => {
    let cancelled = false;
    marketService
      .getSettings()
      .then((settings) => {
        if (!cancelled) {
          setThreshold(settings.thresholdPercent || 3.0);
          setLoading(false);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setLoading(false);
        }
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setSavedMsg("");
    setErrorMsg("");

    if (isNaN(threshold) || threshold < 0.1 || threshold > 50) {
      setErrorMsg("Threshold must be between 0.1% and 50.0%");
      setSaving(false);
      return;
    }

    try {
      const updated = await marketService.updateSettings({ thresholdPercent: threshold });
      setThreshold(updated.thresholdPercent);
      setSavedMsg("Preferences updated successfully! Your attention feed will now use this threshold.");
      setTimeout(() => setSavedMsg(""), 5000);
    } catch (err) {
      setErrorMsg(getErrorMessage(err, "Failed to save settings"));
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="profile-page">
      <header className="page-head">
        <div>
          <p className="eyebrow">User Settings & Preferences</p>
          <h1>Profile</h1>
        </div>
      </header>

      {savedMsg ? <div className="alert ok-alert" style={{ marginBottom: "1rem" }}>{savedMsg}</div> : null}
      {errorMsg ? <div className="alert" style={{ marginBottom: "1rem" }}>{errorMsg}</div> : null}

      <div style={{ display: "grid", gap: "1.2rem", maxWidth: "600px" }}>
        {/* Identity Card */}
        <section className="mini-card">
          <h3>Account Information</h3>
          <p style={{ marginTop: "0.5rem" }}>
            <strong>Name:</strong> {user?.name}
          </p>
          <p>
            <strong>Email:</strong> {user?.email}
          </p>
          <p className="muted" style={{ fontSize: "0.8rem", marginTop: "0.6rem" }}>
            Identity is verified securely through JWT token authentication.
          </p>
        </section>

        {/* Meaningful Change Threshold Settings */}
        <section className="mini-card">
          <h3>MarketPulse Intelligence Preferences</h3>
          <p className="muted" style={{ fontSize: "0.88rem", marginTop: "0.4rem" }}>
            Configure how sensitive MarketPulse should be when highlighting meaningful changes and generating Attention Scores since your last checkpoint.
          </p>

          <form onSubmit={handleSave} style={{ marginTop: "1rem" }}>
            <label htmlFor="threshold-input" className="field-label" style={{ fontWeight: 600 }}>
              Meaningful Movement Threshold (%)
            </label>
            <div style={{ display: "flex", alignItems: "center", gap: "0.8rem", marginTop: "0.3rem" }}>
              <input
                id="threshold-input"
                type="number"
                step="0.1"
                min="0.1"
                max="50.0"
                value={threshold}
                onChange={(e) => setThreshold(parseFloat(e.target.value) || 0)}
                disabled={loading || saving}
                style={{ width: "120px", fontSize: "1.1rem", fontFamily: "IBM Plex Mono, monospace" }}
                required
              />
              <span className="muted" style={{ fontSize: "0.95rem" }}>%</span>
              <button
                type="submit"
                className="action-btn"
                disabled={loading || saving}
              >
                {saving ? "Saving…" : "Save Threshold"}
              </button>
            </div>
            <p className="muted" style={{ fontSize: "0.78rem", marginTop: "0.6rem" }}>
              Default is 3.0%. Price movements exceeding this threshold trigger high attention rankings and "Why it matters" explanations on your Dashboard.
            </p>
          </form>
        </section>
      </div>
    </div>
  );
}

import { useAuth } from "../../features/auth/AuthContext";

export function ProfilePage() {
  const { user } = useAuth();
  return (
    <div>
      <p className="eyebrow">Profile</p>
      <h1>{user?.name}</h1>
      <div className="mini-card">
        <p>
          <strong>Email</strong>
          <br />
          {user?.email}
        </p>
        <p className="muted">Identity comes from the JWT. The API never trusts a user id from the client.</p>
      </div>
    </div>
  );
}

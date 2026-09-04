import { createContext, useContext, useMemo, useState, type ReactNode } from "react";
import { authService } from "../../services/authService";
import type { User } from "../../types";

interface AuthState {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (name: string, email: string, password: string, confirmPassword: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthState | undefined>(undefined);

function readUser(): User | null {
  const raw = localStorage.getItem("mp_user");
  return raw ? (JSON.parse(raw) as User) : null;
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => localStorage.getItem("mp_token"));
  const [user, setUser] = useState<User | null>(readUser);

  const persist = (nextToken: string, nextUser: User) => {
    localStorage.setItem("mp_token", nextToken);
    localStorage.setItem("mp_user", JSON.stringify(nextUser));
    setToken(nextToken);
    setUser(nextUser);
  };

  const value = useMemo<AuthState>(
    () => ({
      user,
      token,
      isAuthenticated: Boolean(token),
      login: async (email, password) => {
        const response = await authService.login({ email, password });
        persist(response.token, response.user);
      },
      register: async (name, email, password, confirmPassword) => {
        const response = await authService.register({ name, email, password, confirmPassword });
        persist(response.token, response.user);
      },
      logout: async () => {
        try {
          await authService.logout();
        } catch {
          // client-side logout still proceeds
        }
        localStorage.removeItem("mp_token");
        localStorage.removeItem("mp_user");
        setToken(null);
        setUser(null);
      },
    }),
    [token, user]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within AuthProvider");
  }
  return context;
}

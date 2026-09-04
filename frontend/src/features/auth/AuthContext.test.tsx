import { act, renderHook } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { authService } from "../../services/authService";
import { AuthProvider, useAuth } from "./AuthContext";

vi.mock("../../services/authService", () => ({
  authService: {
    login: vi.fn(),
    register: vi.fn(),
    logout: vi.fn(),
  },
}));

describe("AuthContext", () => {
  beforeEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
  });

  it("provides initial unauthenticated state when localStorage is empty", () => {
    const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider });
    expect(result.current.isAuthenticated).toBe(false);
    expect(result.current.user).toBeNull();
    expect(result.current.token).toBeNull();
  });

  it("logs in successfully and persists token and user to localStorage", async () => {
    const mockUser = { id: 1, name: "Navya Raj", email: "navya@groww.in" };
    vi.mocked(authService.login).mockResolvedValueOnce({
      token: "jwt-token-123",
      tokenType: "Bearer",
      user: mockUser,
    });

    const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider });

    await act(async () => {
      await result.current.login("navya@groww.in", "password123");
    });

    expect(result.current.isAuthenticated).toBe(true);
    expect(result.current.token).toBe("jwt-token-123");
    expect(result.current.user).toEqual(mockUser);
    expect(localStorage.getItem("mp_token")).toBe("jwt-token-123");
    expect(JSON.parse(localStorage.getItem("mp_user") || "{}")).toEqual(mockUser);
  });

  it("logs out and clears token and user from state and localStorage", async () => {
    const mockUser = { id: 1, name: "Navya Raj", email: "navya@groww.in" };
    localStorage.setItem("mp_token", "existing-token");
    localStorage.setItem("mp_user", JSON.stringify(mockUser));
    vi.mocked(authService.logout).mockResolvedValueOnce({} as any);

    const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider });

    expect(result.current.isAuthenticated).toBe(true);

    await act(async () => {
      await result.current.logout();
    });

    expect(result.current.isAuthenticated).toBe(false);
    expect(result.current.token).toBeNull();
    expect(result.current.user).toBeNull();
    expect(localStorage.getItem("mp_token")).toBeNull();
    expect(localStorage.getItem("mp_user")).toBeNull();
  });
});

import apiClient from "./apiClient";
import type { AuthResponse } from "../types";

export const authService = {
  register: (payload: { name: string; email: string; password: string; confirmPassword: string }) =>
    apiClient.post<AuthResponse>("/api/auth/register", payload).then((res) => res.data),
  login: (payload: { email: string; password: string }) =>
    apiClient.post<AuthResponse>("/api/auth/login", payload).then((res) => res.data),
  logout: () => apiClient.post("/api/auth/logout"),
};

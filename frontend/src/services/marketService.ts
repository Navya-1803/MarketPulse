import apiClient from "./apiClient";
import type { ChangeSummary, DashboardData, MarketQuote } from "../types";

export const marketService = {
  quote: (symbol: string) => apiClient.get<MarketQuote>(`/api/market/${symbol}`).then((res) => res.data),
  dashboard: () => apiClient.get<DashboardData>("/api/dashboard").then((res) => res.data),
  changes: () => apiClient.get<ChangeSummary>("/api/changes").then((res) => res.data),
  acknowledge: () => apiClient.post("/api/checkpoints"),
  health: () => apiClient.get("/api/health").then((res) => res.data),
};

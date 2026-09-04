import apiClient from "./apiClient";
import type { ChangeSummary, DashboardData, MarketQuote, PageResponse } from "../types";

export interface StocksQueryOptions {
  page?: number;
  size?: number;
  query?: string;
}

export const marketService = {
  quote: (symbol: string) => apiClient.get<MarketQuote>(`/api/market/${symbol}`).then((res) => res.data),
  stocks: (params?: StocksQueryOptions) =>
    apiClient.get<PageResponse<MarketQuote>>("/api/stocks", { params }).then((res) => res.data),
  searchStocks: (query: string, params?: { page?: number; size?: number }) =>
    apiClient.get<PageResponse<MarketQuote>>("/api/stocks/search", { params: { query, ...params } }).then((res) => res.data),
  dashboard: () => apiClient.get<DashboardData>("/api/dashboard").then((res) => res.data),
  changes: () => apiClient.get<ChangeSummary>("/api/changes").then((res) => res.data),
  acknowledge: () => apiClient.post("/api/checkpoints"),
  health: () => apiClient.get("/api/health").then((res) => res.data),
};

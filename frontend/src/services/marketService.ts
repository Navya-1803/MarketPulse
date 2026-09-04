import apiClient from "./apiClient";
import type {
  ChangeSummary,
  DashboardData,
  MarketQuote,
  NotificationItem,
  PageResponse,
  StockHistoryResponse,
  UserSettings,
} from "../types";

export interface StocksQueryOptions {
  page?: number;
  size?: number;
  query?: string;
  filter?: string;
  sortBy?: string;
  sortDirection?: string;
}

export const marketService = {
  quote: (symbol: string) =>
    apiClient.get<MarketQuote>(`/api/stocks/${symbol}`).then((res) => res.data),

  history: (symbol: string, range = "1D") =>
    apiClient.get<StockHistoryResponse>(`/api/stocks/${symbol}/history`, { params: { range } }).then((res) => res.data),

  stocks: (params?: StocksQueryOptions) =>
    apiClient.get<PageResponse<MarketQuote>>("/api/stocks", { params }).then((res) => res.data),

  searchStocks: (query: string, params?: { page?: number; size?: number; filter?: string; sortBy?: string; sortDirection?: string }) =>
    apiClient.get<PageResponse<MarketQuote>>("/api/stocks/search", { params: { query, ...params } }).then((res) => res.data),

  dashboard: () => apiClient.get<DashboardData>("/api/dashboard").then((res) => res.data),

  changes: () => apiClient.get<ChangeSummary>("/api/changes").then((res) => res.data),

  acknowledge: () => apiClient.post("/api/checkpoints"),

  health: () => apiClient.get("/api/health").then((res) => res.data),

  getSettings: () => apiClient.get<UserSettings>("/api/settings").then((res) => res.data),

  updateSettings: (settings: UserSettings) =>
    apiClient.put<UserSettings>("/api/settings", settings).then((res) => res.data),

  notifications: () => apiClient.get<NotificationItem[]>("/api/notifications").then((res) => res.data),

  acknowledgeNotification: (id: number) =>
    apiClient.post(`/api/notifications/${id}/acknowledge`),

  acknowledgeAllNotifications: () =>
    apiClient.post("/api/notifications/acknowledge-all"),
};

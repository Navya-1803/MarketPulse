import apiClient from "./apiClient";
import type { Watchlist } from "../types";

export const watchlistService = {
  list: () => apiClient.get<Watchlist[]>("/api/watchlists").then((res) => res.data),
  get: (id: number) => apiClient.get<Watchlist>(`/api/watchlists/${id}`).then((res) => res.data),
  create: (name: string) => apiClient.post<Watchlist>("/api/watchlists", { name }).then((res) => res.data),
  rename: (id: number, name: string) =>
    apiClient.put<Watchlist>(`/api/watchlists/${id}`, { name }).then((res) => res.data),
  remove: (id: number) => apiClient.delete(`/api/watchlists/${id}`),
  addStock: (id: number, symbol: string) =>
    apiClient.post<Watchlist>(`/api/watchlists/${id}/stocks`, { symbol }).then((res) => res.data),
  removeStock: (id: number, symbol: string) => apiClient.delete(`/api/watchlists/${id}/stocks/${symbol}`),
};

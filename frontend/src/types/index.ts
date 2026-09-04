export type MarketStatus = "LIVE" | "DELAYED" | "STALE" | "UNAVAILABLE";
export type ChangeSeverity = "NORMAL" | "NOTABLE" | "SIGNIFICANT" | "CRITICAL";
export type ChangeType = "PRICE_MOVEMENT" | "VOLUME_ANOMALY" | "NEAR_DAY_HIGH" | "NEAR_DAY_LOW";

export interface User {
  id: number;
  name: string;
  email: string;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  user: User;
}

export interface MarketQuote {
  symbol: string;
  companyName: string | null;
  price: number | null;
  changeAmount: number | null;
  changePercent: number | null;
  dayHigh: number | null;
  dayLow: number | null;
  volume: number | null;
  capturedAt: string | null;
  status: MarketStatus;
  message: string | null;
}

export interface WatchlistStock {
  symbol: string;
  addedAt: string;
  quote: MarketQuote | null;
}

export interface Watchlist {
  id: number;
  name: string;
  createdAt: string;
  updatedAt: string;
  stockCount: number;
  stocks: WatchlistStock[];
}

export interface AttentionItem {
  symbol: string;
  companyName: string;
  changeType: ChangeType;
  severity: ChangeSeverity;
  attentionScore: number;
  previousPrice: number;
  currentPrice: number;
  changePercent: number;
  lastCheckedAt: string | null;
  reasons: string[];
  quote: MarketQuote;
}

export interface ChangeSummary {
  lastCheckedAt: string | null;
  meaningfulChangeCount: number;
  items: AttentionItem[];
}

export interface DashboardData {
  user: User;
  changes: ChangeSummary;
  watchlists: Watchlist[];
}

export interface ApiErrorBody {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  details?: string[];
}

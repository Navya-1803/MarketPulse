import { render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { marketService } from "../../services/marketService";
import { watchlistService } from "../../services/watchlistService";
import type { ChangeSummary, MarketQuote, StockHistoryResponse } from "../../types";
import { StockDetailsPage } from "./StockDetailsPage";

vi.mock("../../services/marketService", () => ({
  marketService: {
    quote: vi.fn(),
    changes: vi.fn(),
    history: vi.fn(),
  },
}));

vi.mock("../../services/watchlistService", () => ({
  watchlistService: {
    list: vi.fn(),
    addStock: vi.fn(),
  },
}));

const mockQuote: MarketQuote = {
  symbol: "NVDA",
  companyName: "NVIDIA Corporation",
  price: 178.42,
  changeAmount: 5.42,
  changePercent: 3.21,
  dayHigh: 181.2,
  dayLow: 172.4,
  open: 174.0,
  previousClose: 173.0,
  volume: 48200000,
  currency: "USD",
  exchange: "NASDAQ",
  capturedAt: "2026-09-04T10:42:00Z",
  status: "LIVE",
  message: null,
};

const mockHistory: StockHistoryResponse = {
  symbol: "NVDA",
  range: "1D",
  points: [
    { timestamp: "2026-09-04T09:00:00Z", price: 174.0 },
    { timestamp: "2026-09-04T10:00:00Z", price: 176.5 },
    { timestamp: "2026-09-04T11:00:00Z", price: 178.42 },
  ],
};

const mockChanges: ChangeSummary = {
  lastCheckedAt: "2026-09-04T08:00:00Z",
  meaningfulChangeCount: 1,
  items: [
    {
      symbol: "NVDA",
      companyName: "NVIDIA Corporation",
      changeType: "PRICE_MOVEMENT",
      severity: "SIGNIFICANT",
      attentionScore: 78,
      previousPrice: 170.0,
      currentPrice: 178.42,
      changePercent: 4.95,
      lastCheckedAt: "2026-09-04T08:00:00Z",
      reasons: ["NVDA price increased 4.95% since your last check.", "Move exceeds your configured 3.00% threshold."],
      quote: mockQuote,
    },
  ],
};

describe("StockDetailsPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(marketService.quote).mockResolvedValue(mockQuote);
    vi.mocked(marketService.history).mockResolvedValue(mockHistory);
    vi.mocked(marketService.changes).mockResolvedValue(mockChanges);
    vi.mocked(watchlistService.list).mockResolvedValue([]);
  });

  it("renders stock details, range bar, stats, and MarketPulse insight", async () => {
    render(
      <MemoryRouter initialEntries={["/stocks/NVDA"]}>
        <Routes>
          <Route path="/stocks/:symbol" element={<StockDetailsPage />} />
        </Routes>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByRole("heading", { level: 1, name: "NVDA" })).toBeDefined();
      expect(screen.getByText("NVIDIA Corporation")).toBeDefined();
      expect(screen.getByText("$178.42")).toBeDefined();
    });

    // Range bar labels
    expect(screen.getByText("Today's Trading Range")).toBeDefined();

    // Stats
    expect(screen.getAllByText("Day High").length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText("Day Low").length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText("Open")).toBeDefined();
    expect(screen.getByText("Previous Close")).toBeDefined();

    // Price History chart section
    expect(screen.getByText("Price History")).toBeDefined();

    // MarketPulse Intelligence section
    expect(screen.getByText(/movement since your last check/i)).toBeDefined();
    expect(screen.getByText(/attention score: 78/i)).toBeDefined();
    expect(screen.getByText(/move exceeds your configured 3\.00% threshold\./i)).toBeDefined();
  });
});

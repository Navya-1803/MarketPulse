import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { marketService } from "../../services/marketService";
import { watchlistService } from "../../services/watchlistService";
import type { MarketQuote, PageResponse, Watchlist } from "../../types";
import { StocksPage } from "./StocksPage";

// Mock IntersectionObserver for JSDOM
beforeEach(() => {
  window.IntersectionObserver = vi.fn().mockImplementation(() => ({
    observe: vi.fn(),
    unobserve: vi.fn(),
    disconnect: vi.fn(),
  }));
});

vi.mock("../../services/marketService", () => ({
  marketService: {
    stocks: vi.fn(),
  },
}));

vi.mock("../../services/watchlistService", () => ({
  watchlistService: {
    list: vi.fn(),
    addStock: vi.fn(),
  },
}));

const mockStocks: MarketQuote[] = [
  {
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
  },
  {
    symbol: "TSLA",
    companyName: "Tesla, Inc.",
    price: 340.1,
    changeAmount: -12.5,
    changePercent: -3.55,
    dayHigh: 355.0,
    dayLow: 338.2,
    open: 350.0,
    previousClose: 352.6,
    volume: 89000000,
    currency: "USD",
    exchange: "NASDAQ",
    capturedAt: "2026-09-04T10:42:00Z",
    status: "DELAYED",
    message: null,
  },
  {
    symbol: "AAPL",
    companyName: "Apple Inc.",
    price: 231.21,
    changeAmount: 1.8,
    changePercent: 0.78,
    dayHigh: 233.0,
    dayLow: 230.1,
    open: 230.0,
    previousClose: 229.41,
    volume: 52100000,
    currency: "USD",
    exchange: "NASDAQ",
    capturedAt: "2026-09-04T10:42:00Z",
    status: "LIVE",
    message: null,
  },
];

const mockPageResponse: PageResponse<MarketQuote> = {
  content: mockStocks,
  page: 0,
  size: 25,
  totalElements: 3,
  totalPages: 1,
  hasNext: false,
};

const mockWatchlists: Watchlist[] = [
  {
    id: 1,
    name: "Tech Giants",
    createdAt: "2026-09-01T00:00:00Z",
    updatedAt: "2026-09-01T00:00:00Z",
    stockCount: 2,
    stocks: [],
  },
];

describe("StocksPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(marketService.stocks).mockResolvedValue(mockPageResponse);
    vi.mocked(watchlistService.list).mockResolvedValue(mockWatchlists);
  });

  it("renders Market heading, initial loading state, and all paginated stocks", async () => {
    render(
      <MemoryRouter>
        <StocksPage />
      </MemoryRouter>
    );

    expect(screen.getByText(/loading market data\.\.\./i)).toBeDefined();

    await waitFor(() => {
      expect(screen.getByRole("heading", { level: 1, name: "Market" })).toBeDefined();
      expect(screen.getByPlaceholderText(/search stocks by symbol or company name/i)).toBeDefined();
    });

    expect(screen.getByText("NVDA")).toBeDefined();
    expect(screen.getByText("NVIDIA Corporation")).toBeDefined();
    expect(screen.getByText("$178.42")).toBeDefined();

    expect(screen.getByText("TSLA")).toBeDefined();
    expect(screen.getByText("Tesla, Inc.")).toBeDefined();

    expect(screen.getByText("AAPL")).toBeDefined();
    expect(screen.getByText("Apple Inc.")).toBeDefined();

    expect(screen.getByText(/no more stocks to load\./i)).toBeDefined();
  });

  it("triggers debounced search when user types in search input", async () => {
    render(
      <MemoryRouter>
        <StocksPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText("NVDA")).toBeDefined();
    });

    const searchInput = screen.getByPlaceholderText(/search stocks by symbol or company name/i);

    // Mock search response
    vi.mocked(marketService.stocks).mockResolvedValueOnce({
      content: [mockStocks[1]], // TSLA only
      page: 0,
      size: 25,
      totalElements: 1,
      totalPages: 1,
      hasNext: false,
    });

    fireEvent.change(searchInput, { target: { value: "Tesla" } });

    await waitFor(
      () => {
        expect(marketService.stocks).toHaveBeenCalledWith(
          expect.objectContaining({ query: "Tesla", page: 0 })
        );
      },
      { timeout: 1000 }
    );
  });

  it("opens add to watchlist modal when clicking Add to Watchlist", async () => {
    render(
      <MemoryRouter>
        <StocksPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText("NVDA")).toBeDefined();
    });

    const addButtons = screen.getAllByRole("button", { name: /\+ add to watchlist/i });
    fireEvent.click(addButtons[0]);

    await waitFor(() => {
      expect(screen.getByRole("heading", { level: 2, name: "NVDA" })).toBeDefined();
      expect(screen.getByText("Tech Giants")).toBeDefined();
    });
  });

  it("updates filter when clicking a filter tab", async () => {
    render(
      <MemoryRouter>
        <StocksPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText("NVDA")).toBeDefined();
    });

    const gainersTab = screen.getByRole("tab", { name: /gainers/i });
    fireEvent.click(gainersTab);

    await waitFor(() => {
      expect(marketService.stocks).toHaveBeenCalledWith(
        expect.objectContaining({ filter: "GAINERS", page: 0 })
      );
    });
  });
});


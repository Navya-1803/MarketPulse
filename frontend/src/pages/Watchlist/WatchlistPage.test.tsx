import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { watchlistService } from "../../services/watchlistService";
import type { Watchlist } from "../../types";
import { WatchlistPage } from "./WatchlistPage";

vi.mock("../../services/watchlistService", () => ({
  watchlistService: {
    list: vi.fn(),
    get: vi.fn(),
    create: vi.fn(),
    rename: vi.fn(),
    remove: vi.fn(),
    addStock: vi.fn(),
    removeStock: vi.fn(),
  },
}));

const mockWatchlists: Watchlist[] = [
  {
    id: 10,
    name: "Tech Stocks",
    createdAt: "2026-09-01T10:00:00Z",
    updatedAt: "2026-09-01T10:00:00Z",
    stockCount: 1,
    stocks: [],
  },
];

const mockDetail: Watchlist = {
  id: 10,
  name: "Tech Stocks",
  createdAt: "2026-09-01T10:00:00Z",
  updatedAt: "2026-09-01T10:00:00Z",
  stockCount: 1,
  stocks: [
    {
      symbol: "NVDA",
      addedAt: "2026-09-02T10:00:00Z",
      quote: {
        symbol: "NVDA",
        companyName: "NVIDIA Corporation",
        price: 178.42,
        changeAmount: 5.54,
        changePercent: 3.2,
        dayHigh: 181.2,
        dayLow: 172.4,
        volume: 48200000,
        capturedAt: "2026-09-04T10:00:00Z",
        status: "DELAYED",
        message: null,
      },
    },
  ],
};

describe("WatchlistPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("renders watchlist list in sidebar and empty state when none selected", async () => {
    vi.mocked(watchlistService.list).mockResolvedValueOnce(mockWatchlists);

    render(
      <MemoryRouter initialEntries={["/watchlists"]}>
        <Routes>
          <Route path="/watchlists" element={<WatchlistPage />} />
        </Routes>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText("Tech Stocks")).toBeDefined();
      expect(screen.getByText("1 stocks")).toBeDefined();
      expect(screen.getByText("Select or create a watchlist.")).toBeDefined();
    });
  });

  it("renders stock table display for a selected watchlist", async () => {
    vi.mocked(watchlistService.list).mockResolvedValueOnce(mockWatchlists);
    vi.mocked(watchlistService.get).mockResolvedValueOnce(mockDetail);

    render(
      <MemoryRouter initialEntries={["/watchlists/10"]}>
        <Routes>
          <Route path="/watchlists/:id" element={<WatchlistPage />} />
        </Routes>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByRole("heading", { level: 1, name: "Tech Stocks" })).toBeDefined();
      expect(screen.getByText("NVDA")).toBeDefined();
      expect(screen.getByText("$178.42")).toBeDefined();
      expect(screen.getByText("+3.20%")).toBeDefined();
      expect(screen.getByText(/delayed/i)).toBeDefined();
    });
  });

  it("allows adding a new stock to the active watchlist", async () => {
    vi.mocked(watchlistService.list).mockResolvedValue(mockWatchlists);
    vi.mocked(watchlistService.get).mockResolvedValue(mockDetail);

    const updatedDetail: Watchlist = {
      ...mockDetail,
      stockCount: 2,
      stocks: [
        ...mockDetail.stocks,
        {
          symbol: "AAPL",
          addedAt: "2026-09-04T10:00:00Z",
          quote: null,
        },
      ],
    };

    vi.mocked(watchlistService.addStock).mockResolvedValueOnce(updatedDetail);

    render(
      <MemoryRouter initialEntries={["/watchlists/10"]}>
        <Routes>
          <Route path="/watchlists/:id" element={<WatchlistPage />} />
        </Routes>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByPlaceholderText(/add stock e.g. nvda/i)).toBeDefined();
    });

    const input = screen.getByPlaceholderText(/add stock e.g. nvda/i);
    const submitBtn = screen.getByRole("button", { name: /\+ add stock/i });

    fireEvent.change(input, { target: { value: "AAPL" } });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(watchlistService.addStock).toHaveBeenCalledWith(10, "AAPL");
    });
  });
});

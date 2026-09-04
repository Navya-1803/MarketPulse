import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { useAuth } from "../../features/auth/AuthContext";
import { marketService } from "../../services/marketService";
import type { DashboardData } from "../../types";
import { DashboardPage } from "./DashboardPage";

vi.mock("../../features/auth/AuthContext", () => ({
  useAuth: vi.fn(),
}));

vi.mock("../../services/marketService", () => ({
  marketService: {
    dashboard: vi.fn(),
    acknowledge: vi.fn(),
  },
}));

const mockDashboardData: DashboardData = {
  user: { id: 1, name: "Navya", email: "navya@groww.in" },
  changes: {
    lastCheckedAt: "2026-09-03T18:42:00Z",
    meaningfulChangeCount: 2,
    items: [
      {
        symbol: "NVDA",
        companyName: "NVIDIA Corporation",
        changeType: "PRICE_MOVEMENT",
        severity: "SIGNIFICANT",
        attentionScore: 87,
        previousPrice: 180.0,
        currentPrice: 168.0,
        changePercent: -6.67,
        lastCheckedAt: "2026-09-03T18:42:00Z",
        reasons: [
          "NVDA price decreased 6.67% since your last check.",
          "Move exceeds your configured 3.00% threshold.",
        ],
        quote: {
          symbol: "NVDA",
          companyName: "NVIDIA Corporation",
          price: 168.0,
          changeAmount: -12.0,
          changePercent: -6.67,
          dayHigh: 181.0,
          dayLow: 165.0,
          volume: 50000000,
          capturedAt: "2026-09-04T10:00:00Z",
          status: "LIVE",
          message: null,
        },
      },
      {
        symbol: "TSLA",
        companyName: "Tesla, Inc.",
        changeType: "PRICE_MOVEMENT",
        severity: "NOTABLE",
        attentionScore: 62,
        previousPrice: 340.0,
        currentPrice: 358.0,
        changePercent: 5.29,
        lastCheckedAt: "2026-09-03T18:42:00Z",
        reasons: ["TSLA price increased 5.29% since your last check."],
        quote: {
          symbol: "TSLA",
          companyName: "Tesla, Inc.",
          price: 358.0,
          changeAmount: 18.0,
          changePercent: 5.29,
          dayHigh: 360.0,
          dayLow: 338.0,
          volume: 85000000,
          capturedAt: "2026-09-04T10:00:00Z",
          status: "LIVE",
          message: null,
        },
      },
    ],
  },
  watchlists: [
    {
      id: 1,
      name: "Tech Stocks",
      createdAt: "2026-09-01T10:00:00Z",
      updatedAt: "2026-09-01T10:00:00Z",
      stockCount: 5,
      stocks: [],
    },
  ],
};

describe("DashboardPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(useAuth).mockReturnValue({
      user: { id: 1, name: "Navya", email: "navya@groww.in" },
      token: "test-token",
      isAuthenticated: true,
      login: vi.fn(),
      register: vi.fn(),
      logout: vi.fn(),
    });
  });

  it("renders greeting, last checked time, meaningful changes count, attention cards with scores and reasons", async () => {
    vi.mocked(marketService.dashboard).mockResolvedValueOnce(mockDashboardData);

    render(
      <MemoryRouter>
        <DashboardPage />
      </MemoryRouter>
    );

    // Initial loading indicator
    expect(screen.getByText(/loading your attention feed/i)).toBeDefined();

    // Wait for dashboard data to render
    await waitFor(() => {
      expect(screen.getByText(/navya/i)).toBeDefined();
    });

    // Check heading and meaningful changes counter
    expect(screen.getByText(/2 meaningful changes/i)).toBeDefined();
    expect(screen.getByText(/what changed\?/i)).toBeDefined();
    expect(screen.getByText(/last checked:/i)).toBeDefined();

    // Check attention cards: NVDA and TSLA
    expect(screen.getByText("NVDA")).toBeDefined();
    expect(screen.getByText("NVIDIA Corporation")).toBeDefined();
    expect(screen.getByText("Score 87")).toBeDefined();
    expect(screen.getByText("NVDA price decreased 6.67% since your last check.")).toBeDefined();
    expect(screen.getByText("Move exceeds your configured 3.00% threshold.")).toBeDefined();

    expect(screen.getByText("TSLA")).toBeDefined();
    expect(screen.getByText("Tesla, Inc.")).toBeDefined();
    expect(screen.getByText("Score 62")).toBeDefined();

    // Check watchlist summary
    expect(screen.getByText("Tech Stocks")).toBeDefined();
    expect(screen.getByText("5 stocks")).toBeDefined();
  });

  it("triggers checkpoint update and updates UI when clicking Mark as checked", async () => {
    vi.mocked(marketService.dashboard).mockResolvedValue(mockDashboardData);
    vi.mocked(marketService.acknowledge).mockResolvedValueOnce({
      lastCheckedAt: "2026-09-04T10:45:00Z",
      message: "Checkpoint updated after a successful review.",
    } as any);

    render(
      <MemoryRouter>
        <DashboardPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByRole("button", { name: /mark as checked/i })).toBeDefined();
    });

    const markBtn = screen.getByRole("button", { name: /mark as checked/i });
    fireEvent.click(markBtn);

    await waitFor(() => {
      expect(marketService.acknowledge).toHaveBeenCalledTimes(1);
      expect(screen.getByText(/last checked time updated/i)).toBeDefined();
    });
  });
});

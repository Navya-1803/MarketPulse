import { useEffect, useMemo, useState } from "react";
import { marketService } from "../../services/marketService";
import type { StockHistoryPoint } from "../../types";
import { formatPrice } from "../../utils/format";

interface StockPriceChartProps {
  symbol: string;
}

const RANGES = ["1D", "1W", "1M", "3M"] as const;
type Range = (typeof RANGES)[number];

export function StockPriceChart({ symbol }: StockPriceChartProps) {
  const [range, setRange] = useState<Range>("1D");
  const [points, setPoints] = useState<StockHistoryPoint[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [hoverIndex, setHoverIndex] = useState<number | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(false);
    setHoverIndex(null);

    marketService
      .history(symbol, range)
      .then((res) => {
        if (!cancelled) {
          setPoints(res.points || []);
          setLoading(false);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setError(true);
          setLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [symbol, range]);

  const { minPrice, maxPrice, isUp, pathD, areaD, coordinates } = useMemo(() => {
    if (!points.length) {
      return { minPrice: 0, maxPrice: 0, isUp: true, pathD: "", areaD: "", coordinates: [] };
    }
    const prices = points.map((p) => Number(p.price));
    const min = Math.min(...prices);
    const max = Math.max(...prices);
    const rangeVal = max - min || 1;
    const up = prices[prices.length - 1] >= prices[0];

    const width = 600;
    const height = 220;
    const padding = 20;

    const coords = points.map((p, idx) => {
      const x = padding + (idx / (points.length - 1 || 1)) * (width - padding * 2);
      const y = height - padding - ((Number(p.price) - min) / rangeVal) * (height - padding * 2);
      return { x, y, point: p };
    });

    const linePoints = coords.map((c) => `${c.x.toFixed(1)},${c.y.toFixed(1)}`).join(" L ");
    const linePath = `M ${linePoints}`;
    const first = coords[0];
    const last = coords[coords.length - 1];
    const areaPath = `${linePath} L ${last.x.toFixed(1)},${height} L ${first.x.toFixed(1)},${height} Z`;

    return {
      minPrice: min,
      maxPrice: max,
      isUp: up,
      pathD: linePath,
      areaD: areaPath,
      coordinates: coords,
    };
  }, [points]);

  const strokeColor = isUp ? "#b8f25a" : "#ff6b6b";
  const gradientId = `chart-grad-${symbol}-${range}`;

  const activePoint = hoverIndex != null && coordinates[hoverIndex] ? coordinates[hoverIndex] : null;

  return (
    <div className="stock-chart-card">
      <div className="stock-chart-header">
        <div>
          <h3>Price History</h3>
          <p className="muted">
            {activePoint ? (
              <>
                <strong style={{ color: "var(--text)" }}>{formatPrice(activePoint.point.price)}</strong>
                {" · "}
                <span>{new Date(activePoint.point.timestamp).toLocaleString()}</span>
              </>
            ) : points.length ? (
              <span>Range: {formatPrice(minPrice)} – {formatPrice(maxPrice)}</span>
            ) : null}
          </p>
        </div>
        <div className="chart-range-tabs">
          {RANGES.map((r) => (
            <button
              key={r}
              type="button"
              className={`range-tab-btn ${range === r ? "active" : ""}`}
              onClick={() => setRange(r)}
            >
              {r}
            </button>
          ))}
        </div>
      </div>

      <div className="chart-svg-container">
        {loading ? (
          <div className="chart-loading-placeholder">
            <p className="muted">Loading price chart…</p>
          </div>
        ) : error || !points.length ? (
          <div className="chart-loading-placeholder">
            <p className="muted">Historical chart data temporarily unavailable for {symbol}.</p>
          </div>
        ) : (
          <svg
            viewBox="0 0 600 220"
            className="chart-svg"
            preserveAspectRatio="none"
            onMouseLeave={() => setHoverIndex(null)}
          >
            <defs>
              <linearGradient id={gradientId} x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor={strokeColor} stopOpacity="0.28" />
                <stop offset="100%" stopColor={strokeColor} stopOpacity="0.0" />
              </linearGradient>
            </defs>

            {/* Grid horizontal lines */}
            <line x1="20" y1="20" x2="580" y2="20" stroke="rgba(255,255,255,0.06)" strokeDasharray="3 3" />
            <line x1="20" y1="110" x2="580" y2="110" stroke="rgba(255,255,255,0.06)" strokeDasharray="3 3" />
            <line x1="20" y1="200" x2="580" y2="200" stroke="rgba(255,255,255,0.06)" strokeDasharray="3 3" />

            {/* Area fill */}
            <path d={areaD} fill={`url(#${gradientId})`} />

            {/* Price Line */}
            <path
              d={pathD}
              fill="none"
              stroke={strokeColor}
              strokeWidth="2.2"
              strokeLinecap="round"
              strokeLinejoin="round"
            />

            {/* Invisible hover bars across segments */}
            {coordinates.map((c, idx) => {
              const segWidth = 600 / coordinates.length;
              return (
                <rect
                  key={idx}
                  x={c.x - segWidth / 2}
                  y={0}
                  width={segWidth}
                  height={220}
                  fill="transparent"
                  onMouseEnter={() => setHoverIndex(idx)}
                />
              );
            })}

            {/* Active hover crosshair point */}
            {activePoint ? (
              <g>
                <line
                  x1={activePoint.x}
                  y1={15}
                  x2={activePoint.x}
                  y2={205}
                  stroke="rgba(255,255,255,0.25)"
                  strokeDasharray="2 2"
                />
                <circle
                  cx={activePoint.x}
                  cy={activePoint.y}
                  r="5"
                  fill={strokeColor}
                  stroke="#081410"
                  strokeWidth="2"
                />
              </g>
            ) : null}
          </svg>
        )}
      </div>
    </div>
  );
}

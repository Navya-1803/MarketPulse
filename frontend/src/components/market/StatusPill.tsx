import type { MarketStatus } from "../../types";
import { statusLabel } from "../../utils/format";

export function StatusPill({ status }: { status: MarketStatus }) {
  return <span className={`status-pill status-${status.toLowerCase()}`}>{statusLabel(status)}</span>;
}

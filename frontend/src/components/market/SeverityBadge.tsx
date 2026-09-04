import type { ChangeSeverity } from "../../types";

const labels: Record<ChangeSeverity, string> = {
  CRITICAL: "High attention",
  SIGNIFICANT: "Significant",
  NOTABLE: "Notable",
  NORMAL: "Watch",
};

export function SeverityBadge({ severity }: { severity: ChangeSeverity }) {
  return <span className={`severity severity-${severity.toLowerCase()}`}>{labels[severity]}</span>;
}

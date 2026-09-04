import { describe, expect, it } from "vitest";
import { formatPercent } from "./utils/format";

describe("formatPercent", () => {
  it("adds a plus sign for positive moves", () => {
    expect(formatPercent(5.88)).toBe("+5.88%");
  });
});

package airhacks.zdmd.tokens.entity;

/**
 * A parsed dimension like "42px" or "1.5rem". Standard units are px, em,
 * and rem; others are preserved but flagged by the linter. Unitless
 * line-height multipliers carry an empty unit.
 */
public record ResolvedDimension(double value, String unit) {
}

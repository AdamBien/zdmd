package airhacks.zdmd.tokens.entity;

/**
 * A parsed CSS color in sRGB with WCAG relative luminance.
 * {@code alpha} is null when the source color had no alpha channel.
 */
public record ResolvedColor(String hex, int r, int g, int b, Double alpha, double luminance) {
}

package airhacks.zdmd.tokens.entity;

/** A typography scale; every property is optional (null when absent). */
public record ResolvedTypography(
        String fontFamily,
        ResolvedDimension fontSize,
        Double fontWeight,
        ResolvedDimension lineHeight,
        ResolvedDimension letterSpacing,
        String fontFeature,
        String fontVariation) {
}

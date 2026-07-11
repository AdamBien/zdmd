package airhacks.zdmd.tokens.entity;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The DESIGN.md format specification constants — the Java rendition of the
 * upstream spec-config.yaml, which is the single source of truth for the
 * format. Values must stay aligned with that file.
 */
public interface Spec {

    String VERSION = "alpha";

    int MAX_TOKEN_NESTING_DEPTH = 20;
    int MAX_REFERENCE_DEPTH = 10;

    /** Canonical top-level YAML keys per the DESIGN.md schema. */
    List<String> SCHEMA_KEYS = List.of(
            "version", "name", "description", "colors", "typography", "rounded", "spacing", "components");

    /** Units the spec formally supports for Dimension values. */
    Set<String> STANDARD_UNITS = Set.of("px", "em", "rem");

    /** All known CSS length/percentage units. Adding a new CSS unit = one string here. */
    Set<String> CSS_UNITS = Set.of(
            "px", "cm", "mm", "in", "pt", "pc",
            "em", "rem", "ex", "ch", "cap", "ic", "lh", "rlh",
            "vh", "vw", "vmin", "vmax",
            "dvh", "dvw", "dvmin", "dvmax",
            "svh", "svw", "svmin", "svmax",
            "lvh", "lvw", "lvmin", "lvmax",
            "cqw", "cqh", "cqi", "cqb", "cqmin", "cqmax",
            "%");

    List<String> VALID_TYPOGRAPHY_PROPS = List.of(
            "fontFamily", "fontSize", "fontWeight", "lineHeight", "letterSpacing", "fontFeature", "fontVariation");

    List<String> VALID_COMPONENT_SUB_TOKENS = List.of(
            "backgroundColor", "textColor", "typography", "rounded", "padding", "size", "height", "width");

    /** Ordered list of canonical section names. */
    List<String> CANONICAL_ORDER = List.of(
            "Overview", "Colors", "Typography", "Layout", "Elevation & Depth", "Shapes", "Components",
            "Do's and Don'ts");

    /** Alias heading → canonical name. */
    Map<String, String> SECTION_ALIASES = Map.of(
            "Brand & Style", "Overview",
            "Layout & Spacing", "Layout",
            "Elevation", "Elevation & Depth");

    static String resolveAlias(String heading) {
        return SECTION_ALIASES.getOrDefault(heading, heading);
    }
}

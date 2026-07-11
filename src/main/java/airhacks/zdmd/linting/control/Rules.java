package airhacks.zdmd.linting.control;

import airhacks.zdmd.tokens.boundary.TokenModel;
import airhacks.zdmd.tokens.entity.DesignSystem;
import airhacks.zdmd.tokens.entity.Finding;
import airhacks.zdmd.tokens.entity.ResolvedColor;
import airhacks.zdmd.tokens.entity.Spec;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The default lint rules, executed in the upstream order. Each rule is a
 * pure function from the design system state to findings.
 */
public interface Rules {

    static List<Finding> run(DesignSystem state) {
        var findings = new ArrayList<Finding>();
        findings.addAll(brokenRef(state));
        findings.addAll(missingPrimary(state));
        findings.addAll(contrastCheck(state));
        findings.addAll(orphanedTokens(state));
        findings.addAll(tokenSummary(state));
        findings.addAll(missingSections(state));
        findings.addAll(missingTypography(state));
        findings.addAll(sectionOrder(state));
        findings.addAll(unknownKey(state));
        findings.addAll(tokenLikeIgnored(state));
        return findings;
    }

    /** Broken/circular references (error) and unknown component sub-tokens (warning). */
    static List<Finding> brokenRef(DesignSystem state) {
        var findings = new ArrayList<Finding>();
        state.components().forEach((componentName, component) -> {
            for (var reference : component.unresolvedRefs()) {
                findings.add(Finding.error("components." + componentName,
                        "Reference %s does not resolve to any defined token.".formatted(reference)));
            }
            for (var propName : component.properties().keySet()) {
                if (!Spec.VALID_COMPONENT_SUB_TOKENS.contains(propName)) {
                    findings.add(Finding.warning("components.%s.%s".formatted(componentName, propName),
                            "'%s' is not a recognized component sub-token. Valid sub-tokens: %s."
                                    .formatted(propName, String.join(", ", Spec.VALID_COMPONENT_SUB_TOKENS))));
                }
            }
        });
        return findings;
    }

    /** Missing primary color — warns when colors are defined but no 'primary' exists. */
    static List<Finding> missingPrimary(DesignSystem state) {
        if (!state.colors().isEmpty() && !state.colors().containsKey("primary")) {
            return List.of(Finding.warning("colors",
                    "No 'primary' color defined. The agent will auto-generate key colors, reducing your control over the palette."));
        }
        return List.of();
    }

    double WCAG_AA_MINIMUM = 4.5;

    /** WCAG contrast ratio — warns when backgroundColor/textColor pairs fall below AA. */
    static List<Finding> contrastCheck(DesignSystem state) {
        var findings = new ArrayList<Finding>();
        state.components().forEach((componentName, component) -> {
            if (component.properties().get("backgroundColor") instanceof ResolvedColor background
                    && component.properties().get("textColor") instanceof ResolvedColor text) {
                var ratio = TokenModel.contrastRatio(background, text);
                if (ratio < WCAG_AA_MINIMUM) {
                    findings.add(Finding.warning("components." + componentName,
                            "textColor (%s) on backgroundColor (%s) has contrast ratio %s:1, below WCAG AA minimum of %s:1."
                                    .formatted(text.hex(), background.hex(),
                                            String.format(Locale.ROOT, "%.2f", ratio), WCAG_AA_MINIMUM)));
                }
            }
        });
        return findings;
    }

    /**
     * Material Design 3 baseline color families — part of the MD3 standard
     * contract, never flagged as orphaned.
     */
    Set<String> MD3_STANDARD_FAMILIES = Set.of(
            "primary", "secondary", "tertiary", "error", "surface", "background", "outline");

    /** Orphaned tokens — defined but never referenced by any component or MD3 sibling. */
    static List<Finding> orphanedTokens(DesignSystem state) {
        if (state.components().isEmpty()) {
            return List.of();
        }
        var referencedPaths = new HashSet<String>();
        for (var component : state.components().values()) {
            for (var value : component.properties().values()) {
                if (value instanceof ResolvedColor || value instanceof airhacks.zdmd.tokens.entity.ResolvedDimension
                        || value instanceof airhacks.zdmd.tokens.entity.ResolvedTypography) {
                    state.symbolTable().forEach((key, symbolValue) -> {
                        if (symbolValue == value) {
                            referencedPaths.add(key);
                        }
                    });
                }
            }
        }

        var referencedFamilies = new HashSet<String>();
        for (var path : referencedPaths) {
            if (path.startsWith("colors.")) {
                referencedFamilies.add(colorFamily(path.substring("colors.".length())));
            }
        }

        var findings = new ArrayList<Finding>();
        for (var name : state.colors().keySet()) {
            var path = "colors." + name;
            if (referencedPaths.contains(path)) {
                continue;
            }
            var family = colorFamily(name);
            if (referencedFamilies.contains(family) || MD3_STANDARD_FAMILIES.contains(family)) {
                continue;
            }
            findings.add(Finding.warning(path, "'%s' is defined but never referenced by any component.".formatted(name)));
        }
        return findings;
    }

    /**
     * Reduce a Material Design 3 color token name to its family root by
     * stripping MD3 prefixes (on-, inverse-) and suffixes (-container*,
     * -fixed*, -dim, -bright, -tint, -variant).
     */
    private static String colorFamily(String name) {
        var n = name;
        n = n.replaceFirst("^on-", "");
        n = n.replaceFirst("^inverse-", "");
        n = n.replaceFirst("^on-", "");
        n = n.replaceFirst("-container.*$", "");
        n = n.replaceFirst("-fixed.*$", "");
        n = n.replaceFirst("-(dim|bright|tint|variant)$", "");
        return n;
    }

    /** Token count summary — info diagnostic with per-section counts. */
    static List<Finding> tokenSummary(DesignSystem state) {
        var parts = new ArrayList<String>();
        addCount(parts, state.colors().size(), "color", "colors");
        addCount(parts, state.typography().size(), "typography scale", "typography scales");
        addCount(parts, state.rounded().size(), "rounding level", "rounding levels");
        addCount(parts, state.spacing().size(), "spacing token", "spacing tokens");
        addCount(parts, state.components().size(), "component", "components");
        if (parts.isEmpty()) {
            return List.of();
        }
        return List.of(Finding.info(null, "Design system defines %s.".formatted(String.join(", ", parts))));
    }

    private static void addCount(List<String> parts, int size, String singular, String plural) {
        if (size > 0) {
            parts.add(size + " " + (size == 1 ? singular : plural));
        }
    }

    /** Missing sections — notes when optional sections (spacing, rounded) are absent. */
    static List<Finding> missingSections(DesignSystem state) {
        var findings = new ArrayList<Finding>();
        if (state.spacing().isEmpty() && !state.colors().isEmpty()) {
            findings.add(Finding.info("spacing",
                    "No 'spacing' section defined. Layout spacing will fall back to agent defaults."));
        }
        if (state.rounded().isEmpty() && !state.colors().isEmpty()) {
            findings.add(Finding.info("rounded",
                    "No 'rounded' section defined. Corner rounding will fall back to agent defaults."));
        }
        return findings;
    }

    /** Missing typography — warns when colors exist but no typography tokens. */
    static List<Finding> missingTypography(DesignSystem state) {
        if (state.typography().isEmpty() && !state.colors().isEmpty()) {
            return List.of(Finding.warning("typography",
                    "No typography tokens defined. Agents will use default font choices, reducing your control over the design system's typographic identity."));
        }
        return List.of();
    }

    /** Section order — warns when known sections are out of canonical order. */
    static List<Finding> sectionOrder(DesignSystem state) {
        var sections = state.sections() == null ? List.<String>of() : state.sections();
        if (sections.isEmpty()) {
            return List.of();
        }
        var known = sections.stream()
                .map(Spec::resolveAlias)
                .filter(Spec.CANONICAL_ORDER::contains)
                .toList();
        for (var i = 0; i < known.size() - 1; i++) {
            var currentIndex = Spec.CANONICAL_ORDER.indexOf(known.get(i));
            var nextIndex = Spec.CANONICAL_ORDER.indexOf(known.get(i + 1));
            if (currentIndex > nextIndex) {
                return List.of(Finding.warning(null,
                        "Section '%s' appears before '%s', which is out of order. Expected order: %s"
                                .formatted(known.get(i), known.get(i + 1), String.join(", ", Spec.CANONICAL_ORDER))));
            }
        }
        return List.of();
    }

    /** Max edit distance to consider an unknown key a typo (not a custom key). */
    int MAX_TYPO_DISTANCE = 2;

    /** Unknown key — warns when a top-level YAML key looks like a typo of a schema key. */
    static List<Finding> unknownKey(DesignSystem state) {
        var findings = new ArrayList<Finding>();
        for (var key : state.unknownKeys()) {
            String bestMatch = null;
            var bestDistance = Integer.MAX_VALUE;
            for (var knownKey : Spec.SCHEMA_KEYS) {
                if (Math.abs(key.length() - knownKey.length()) > MAX_TYPO_DISTANCE) {
                    continue;
                }
                var distance = Levenshtein.distance(key.toLowerCase(), knownKey.toLowerCase());
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestMatch = knownKey;
                }
            }
            if (bestDistance <= MAX_TYPO_DISTANCE && bestMatch != null) {
                findings.add(Finding.warning(key,
                        "Unknown key \"%s\" — did you mean \"%s\"?".formatted(key, bestMatch)));
            }
        }
        return findings;
    }

    Pattern HEX_COLOR = Pattern.compile("^#([0-9a-fA-F]{3,4}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})$");
    Pattern CSS_DIMENSION = Pattern.compile("^-?\\d*\\.?\\d+[a-zA-Z%]+$");
    Set<String> TYPOGRAPHY_PROPS = Set.of("fontFamily", "fontSize", "fontWeight", "lineHeight", "letterSpacing");
    int MAX_TOKEN_VALUE_LENGTH = 64;

    /**
     * Token-like ignored keys — warns when an unknown top-level key holds
     * what looks like a design-token map that export would silently drop.
     */
    static List<Finding> tokenLikeIgnored(DesignSystem state) {
        var findings = new ArrayList<Finding>();
        for (var key : state.unknownKeys()) {
            if (state.unknownKeyValues().get(key) instanceof Map<?, ?> value && hasTokenLikeContent(value)) {
                findings.add(Finding.warning(key,
                        ("\"%s\" looks like a design-token map but is not a recognized schema key "
                                + "(colors, typography, spacing, rounded, components). "
                                + "It will be silently ignored by export commands. "
                                + "Rename it to a supported key or move its values under a recognized section.")
                                .formatted(key)));
            }
        }
        return findings;
    }

    private static boolean hasTokenLikeContent(Map<?, ?> tokens) {
        for (var entry : tokens.entrySet()) {
            if (TYPOGRAPHY_PROPS.contains(String.valueOf(entry.getKey()))) {
                return true;
            }
            switch (entry.getValue()) {
                case String text -> {
                    if (text.length() <= MAX_TOKEN_VALUE_LENGTH
                            && (HEX_COLOR.matcher(text).matches() || CSS_DIMENSION.matcher(text).matches())) {
                        return true;
                    }
                }
                case Map<?, ?> nested -> {
                    if (hasTokenLikeContent(nested)) {
                        return true;
                    }
                }
                default -> {
                }
            }
        }
        return false;
    }
}

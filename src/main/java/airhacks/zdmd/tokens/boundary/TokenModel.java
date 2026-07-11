package airhacks.zdmd.tokens.boundary;

import airhacks.zdmd.parsing.entity.ParsedDesignSystem;
import airhacks.zdmd.tokens.control.CssColors;
import airhacks.zdmd.tokens.control.Dimensions;
import airhacks.zdmd.tokens.entity.ComponentDef;
import airhacks.zdmd.tokens.entity.DesignSystem;
import airhacks.zdmd.tokens.entity.Finding;
import airhacks.zdmd.tokens.entity.ModelResult;
import airhacks.zdmd.tokens.entity.ResolvedColor;
import airhacks.zdmd.tokens.entity.ResolvedDimension;
import airhacks.zdmd.tokens.entity.ResolvedTypography;
import airhacks.zdmd.tokens.entity.Spec;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Builds a resolved DesignSystem from parsed YAML tokens: color parsing,
 * dimension parsing, typography construction, and chained token reference
 * resolution with cycle detection. Never throws — all errors are returned
 * as findings.
 */
public interface TokenModel {

    static ModelResult resolve(ParsedDesignSystem input) {
        var findings = new ArrayList<Finding>();
        var symbolTable = new LinkedHashMap<String, Object>();
        var colors = new LinkedHashMap<String, ResolvedColor>();
        var typography = new LinkedHashMap<String, ResolvedTypography>();
        var rounded = new LinkedHashMap<String, ResolvedDimension>();
        var spacing = new LinkedHashMap<String, ResolvedDimension>();

        // ── Phase 1: resolve primitive tokens ───────────────────────
        if (input.colors() != null) {
            forEachLeaf(input.colors(), (name, raw) -> {
                if (Dimensions.isTokenReference(raw)) {
                    symbolTable.put("colors." + name, raw);
                    return;
                }
                var resolved = raw instanceof String text ? CssColors.parse(text) : null;
                if (resolved != null) {
                    colors.put(name, resolved);
                    symbolTable.put("colors." + name, resolved);
                } else {
                    findings.add(Finding.error("colors." + name,
                            "'%s' is not a valid color. Expected a CSS color value (e.g., #ffffff, rgb(0 0 0), oklch(0.5 0.2 240))."
                                    .formatted(raw)));
                    symbolTable.put("colors." + name, raw);
                }
            }, findings, "colors");
        }

        if (input.typography() != null) {
            for (var entry : input.typography().entrySet()) {
                if (!(entry.getValue() instanceof Map<?, ?> props)) {
                    continue;
                }
                var resolved = parseTypography(props, "typography." + entry.getKey(), findings);
                typography.put(entry.getKey(), resolved);
                symbolTable.put("typography." + entry.getKey(), resolved);
            }
        }

        if (input.rounded() != null) {
            forEachLeaf(input.rounded(), (name, raw) -> {
                if (!(raw instanceof String text)) {
                    return;
                }
                if (Dimensions.isParseable(text)) {
                    var resolved = Dimensions.parse(text);
                    if (!Spec.STANDARD_UNITS.contains(resolved.unit())) {
                        findings.add(Finding.error("rounded." + name,
                                "'%s' has an invalid unit '%s'. Only px, rem, and em are allowed."
                                        .formatted(text, resolved.unit())));
                    }
                    rounded.put(name, resolved);
                    symbolTable.put("rounded." + name, resolved);
                } else if (!Dimensions.isTokenReference(text)) {
                    findings.add(Finding.error("rounded." + name, "'%s' is not a valid dimension.".formatted(text)));
                    symbolTable.put("rounded." + name, text);
                } else {
                    symbolTable.put("rounded." + name, text);
                }
            }, findings, "rounded");
        }

        if (input.spacing() != null) {
            forEachLeaf(input.spacing(), (name, raw) -> {
                if (Dimensions.isParseable(raw)) {
                    var resolved = Dimensions.parse((String) raw);
                    spacing.put(name, resolved);
                    symbolTable.put("spacing." + name, resolved);
                } else {
                    symbolTable.put("spacing." + name, raw);
                }
            }, findings, "spacing");
        }

        // ── Phase 2: resolve chained references ─────────────────────
        if (input.colors() != null) {
            forEachLeaf(input.colors(), (name, raw) -> {
                if (Dimensions.isTokenReference(raw)) {
                    var resolved = resolveReference(symbolTable, Dimensions.referencePath((String) raw),
                            new HashSet<>(), 0);
                    if (resolved instanceof ResolvedColor color) {
                        colors.put(name, color);
                        symbolTable.put("colors." + name, color);
                    }
                }
            }, findings, null);
        }

        if (input.rounded() != null) {
            forEachLeaf(input.rounded(), (name, raw) -> {
                if (Dimensions.isTokenReference(raw)) {
                    var resolved = resolveReference(symbolTable, Dimensions.referencePath((String) raw),
                            new HashSet<>(), 0);
                    if (resolved instanceof ResolvedDimension dimension) {
                        rounded.put(name, dimension);
                        symbolTable.put("rounded." + name, dimension);
                    }
                }
            }, findings, null);
        }

        if (input.spacing() != null) {
            forEachLeaf(input.spacing(), (name, raw) -> {
                if (Dimensions.isTokenReference(raw)) {
                    var resolved = resolveReference(symbolTable, Dimensions.referencePath((String) raw),
                            new HashSet<>(), 0);
                    if (resolved instanceof ResolvedDimension dimension) {
                        spacing.put(name, dimension);
                        symbolTable.put("spacing." + name, dimension);
                    }
                }
            }, findings, null);
        }

        // ── Phase 3: build components ────────────────────────────────
        var components = new LinkedHashMap<String, ComponentDef>();
        if (input.components() != null) {
            for (var componentEntry : input.components().entrySet()) {
                if (!(componentEntry.getValue() instanceof Map<?, ?> props)) {
                    continue;
                }
                var properties = new LinkedHashMap<String, Object>();
                var unresolvedRefs = new ArrayList<String>();

                for (var propEntry : props.entrySet()) {
                    var propName = String.valueOf(propEntry.getKey());
                    var rawValue = propEntry.getValue();
                    if (rawValue instanceof Number || rawValue instanceof Boolean) {
                        properties.put(propName, rawValue);
                    } else if (Dimensions.isTokenReference(rawValue)) {
                        var reference = (String) rawValue;
                        var resolved = resolveReference(symbolTable, Dimensions.referencePath(reference),
                                new HashSet<>(), 0);
                        if (resolved != null) {
                            properties.put(propName, resolved);
                        } else {
                            unresolvedRefs.add(reference);
                            properties.put(propName, reference);
                        }
                    } else if (rawValue instanceof String text && CssColors.parse(text) != null) {
                        properties.put(propName, CssColors.parse(text));
                    } else if (Dimensions.isParseable(rawValue)) {
                        properties.put(propName, Dimensions.parse((String) rawValue));
                    } else {
                        properties.put(propName, rawValue);
                    }
                }
                components.put(componentEntry.getKey(), new ComponentDef(properties, unresolvedRefs));
            }
        }

        var unknownKeys = input.sourceMap().keySet().stream()
                .filter(key -> !Spec.SCHEMA_KEYS.contains(key))
                .toList();

        var unknownKeyValues = new LinkedHashMap<String, Object>();
        if (input.rawValues() != null) {
            for (var key : unknownKeys) {
                if (input.rawValues().containsKey(key)) {
                    unknownKeyValues.put(key, input.rawValues().get(key));
                }
            }
        }

        var designSystem = new DesignSystem(input.name(), input.description(), colors, typography, rounded, spacing,
                components, symbolTable, input.sections(), unknownKeys, unknownKeyValues);
        return new ModelResult(designSystem, findings);
    }

    /** WCAG 2.1 contrast ratio between two resolved colors. */
    static double contrastRatio(ResolvedColor a, ResolvedColor b) {
        var lighter = Math.max(a.luminance(), b.luminance());
        var darker = Math.min(a.luminance(), b.luminance());
        return (lighter + 0.05) / (darker + 0.05);
    }

    private static ResolvedTypography parseTypography(Map<?, ?> props, String path, List<Finding> findings) {
        String fontFamily = null;
        Double fontWeight = null;
        String fontFeature = null;
        String fontVariation = null;

        if (props.get("fontFamily") instanceof String family) {
            if (CssColors.parse(family) != null) {
                findings.add(Finding.error(path + ".fontFamily",
                        "'%s' appears to be a color, not a valid font family.".formatted(family)));
            }
            fontFamily = family;
        }
        var rawWeight = props.get("fontWeight");
        if (rawWeight != null) {
            fontWeight = switch (rawWeight) {
                case Number number -> number.doubleValue();
                case String text -> numberOrNull(text);
                default -> null;
            };
            if (fontWeight == null) {
                findings.add(Finding.error(path + ".fontWeight",
                        "'%s' is not a valid font weight. Expected a number.".formatted(rawWeight)));
            }
        }
        if (props.get("fontFeature") instanceof String feature) {
            fontFeature = feature;
        }
        if (props.get("fontVariation") instanceof String variation) {
            fontVariation = variation;
        }

        var fontSize = typographyDimension(props, "fontSize", path, findings);
        var lineHeight = typographyDimension(props, "lineHeight", path, findings);
        var letterSpacing = typographyDimension(props, "letterSpacing", path, findings);

        return new ResolvedTypography(fontFamily, fontSize, fontWeight, lineHeight, letterSpacing,
                fontFeature, fontVariation);
    }

    private static ResolvedDimension typographyDimension(Map<?, ?> props, String prop, String path,
            List<Finding> findings) {
        if (!(props.get(prop) instanceof String raw)) {
            return null;
        }
        if (Dimensions.isParseable(raw)) {
            var parsed = Dimensions.parse(raw);
            if (!Spec.STANDARD_UNITS.contains(parsed.unit())) {
                findings.add(Finding.error(path + "." + prop,
                        "'%s' has an invalid unit '%s'. Only px, rem, and em are allowed."
                                .formatted(raw, parsed.unit())));
            }
            return parsed;
        }
        if (prop.equals("lineHeight") && raw.matches("\\d*\\.?\\d+")) {
            return new ResolvedDimension(Double.parseDouble(raw), "");
        }
        if (!Dimensions.isTokenReference(raw)) {
            findings.add(Finding.error(path + "." + prop, "'%s' is not a valid dimension.".formatted(raw)));
        }
        return null;
    }

    private static Double numberOrNull(String text) {
        try {
            return Double.valueOf(text.strip());
        } catch (NumberFormatException notANumber) {
            return null;
        }
    }

    /**
     * Resolve a token reference with chained resolution and cycle detection.
     * Returns null if the reference cannot be resolved (not found or circular).
     */
    private static Object resolveReference(Map<String, Object> symbolTable, String path, Set<String> visited,
            int depth) {
        if (depth > Spec.MAX_REFERENCE_DEPTH || visited.contains(path)) {
            return null;
        }
        visited.add(path);
        var value = symbolTable.get(path);
        if (value == null) {
            return null;
        }
        if (Dimensions.isTokenReference(value)) {
            return resolveReference(symbolTable, Dimensions.referencePath((String) value), visited, depth + 1);
        }
        return value;
    }

    /**
     * Recursively iterate over a token map and call the consumer for each leaf.
     * Leaf paths are dot-separated (e.g. "background.light"). Nested maps
     * recurse; lists and scalars are leaves.
     */
    private static void forEachLeaf(Map<String, Object> tokens, BiConsumer<String, Object> consumer,
            List<Finding> findings, String rootPath) {
        forEachLeaf(tokens, consumer, "", 0, findings, rootPath);
    }

    private static void forEachLeaf(Map<String, Object> tokens, BiConsumer<String, Object> consumer, String prefix,
            int depth, List<Finding> findings, String rootPath) {
        if (depth > Spec.MAX_TOKEN_NESTING_DEPTH) {
            if (rootPath != null && findings.stream()
                    .noneMatch(f -> rootPath.equals(f.path()) && f.message().contains("nesting depth"))) {
                findings.add(Finding.error(rootPath,
                        "Token nesting depth exceeds maximum allowed depth of %d.".formatted(Spec.MAX_TOKEN_NESTING_DEPTH)));
            }
            return;
        }
        for (var entry : tokens.entrySet()) {
            var fullPath = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            if (entry.getValue() instanceof Map<?, ?> nested) {
                @SuppressWarnings("unchecked")
                var nestedTokens = (Map<String, Object>) nested;
                forEachLeaf(nestedTokens, consumer, fullPath, depth + 1, findings, rootPath);
            } else {
                consumer.accept(fullPath, entry.getValue());
            }
        }
    }
}

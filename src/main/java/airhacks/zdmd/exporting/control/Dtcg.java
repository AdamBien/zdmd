package airhacks.zdmd.exporting.control;

import airhacks.zdmd.tokens.entity.DesignSystem;
import airhacks.zdmd.tokens.entity.ResolvedColor;
import airhacks.zdmd.tokens.entity.ResolvedDimension;
import airhacks.zdmd.tokens.entity.ResolvedTypography;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps a design system to a W3C Design Tokens (DTCG Format Module 2025.10)
 * tokens.json structure. Pure function — no I/O.
 */
public interface Dtcg {

    String SCHEMA_URL = "https://www.designtokens.org/schemas/2025.10/format.json";

    static Map<String, Object> tokenFile(DesignSystem state) {
        var file = new LinkedHashMap<String, Object>();
        file.put("$schema", SCHEMA_URL);

        if (state.name() != null || state.description() != null) {
            file.put("$description", state.description() != null ? state.description() : state.name());
        }

        if (!state.colors().isEmpty()) {
            var group = new LinkedHashMap<String, Object>();
            group.put("$type", "color");
            state.colors().forEach((name, color) -> group.put(name, token(colorValue(color))));
            file.put("color", group);
        }

        dimensionGroup(file, "spacing", state.spacing());
        dimensionGroup(file, "rounded", state.rounded());

        if (!state.typography().isEmpty()) {
            var group = new LinkedHashMap<String, Object>();
            state.typography().forEach((name, typography) -> {
                var token = new LinkedHashMap<String, Object>();
                token.put("$type", "typography");
                token.put("$value", typographyValue(typography));
                group.put(name, token);
            });
            file.put("typography", group);
        }

        return file;
    }

    private static void dimensionGroup(Map<String, Object> file, String groupName,
            Map<String, ResolvedDimension> dimensions) {
        if (dimensions.isEmpty()) {
            return;
        }
        var group = new LinkedHashMap<String, Object>();
        group.put("$type", "dimension");
        dimensions.forEach((name, dimension) -> group.put(name, token(dimensionValue(dimension))));
        file.put(groupName, group);
    }

    private static Map<String, Object> token(Object value) {
        var token = new LinkedHashMap<String, Object>();
        token.put("$value", value);
        return token;
    }

    private static Map<String, Object> colorValue(ResolvedColor color) {
        var value = new LinkedHashMap<String, Object>();
        value.put("colorSpace", "srgb");
        value.put("components", List.of(
                round(color.r() / 255.0),
                round(color.g() / 255.0),
                round(color.b() / 255.0)));
        value.put("hex", color.hex().toLowerCase());
        return value;
    }

    private static Map<String, Object> dimensionValue(ResolvedDimension dimension) {
        var value = new LinkedHashMap<String, Object>();
        value.put("value", dimension.value());
        value.put("unit", dimension.unit());
        return value;
    }

    private static Map<String, Object> typographyValue(ResolvedTypography typography) {
        var value = new LinkedHashMap<String, Object>();
        if (typography.fontFamily() != null) {
            value.put("fontFamily", typography.fontFamily());
        }
        if (typography.fontSize() != null) {
            value.put("fontSize", dimensionValue(typography.fontSize()));
        }
        if (typography.fontWeight() != null) {
            value.put("fontWeight", typography.fontWeight());
        }
        if (typography.letterSpacing() != null) {
            value.put("letterSpacing", dimensionValue(typography.letterSpacing()));
        }
        if (typography.lineHeight() != null) {
            // DTCG lineHeight is a unitless multiplier of fontSize
            value.put("lineHeight", typography.lineHeight().value());
        }
        return value;
    }

    /** Round to 3 decimal places for clean output. */
    private static double round(double value) {
        return Math.round(value * 1000) / 1000.0;
    }
}

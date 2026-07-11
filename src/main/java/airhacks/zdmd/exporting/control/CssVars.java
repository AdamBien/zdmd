package airhacks.zdmd.exporting.control;

import airhacks.zdmd.reporting.boundary.Json;
import airhacks.zdmd.tokens.entity.DesignSystem;
import airhacks.zdmd.tokens.entity.ResolvedDimension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Maps a design system to CSS custom property declarations and serializes
 * them into a `:root { ... }` block. Pure functions — no I/O.
 */
public interface CssVars {

    record Declaration(String name, String value) {
    }

    static List<Declaration> declarations(DesignSystem state) {
        var declarations = new ArrayList<Declaration>();
        state.colors().forEach((name, color) ->
                declarations.add(new Declaration("color-" + cssSafe(name), color.hex().toLowerCase())));
        mapDimensionGroup(declarations, "spacing", state.spacing());
        mapDimensionGroup(declarations, "rounded", state.rounded());
        return declarations;
    }

    private static void mapDimensionGroup(List<Declaration> declarations, String group,
            Map<String, ResolvedDimension> dimensions) {
        dimensions.forEach((name, dimension) ->
                declarations.add(new Declaration(group + "-" + cssSafe(name),
                        Json.number(dimension.value()) + dimension.unit())));
    }

    /**
     * Nested tokens flatten to dotted keys; a literal dot makes a browser
     * drop the declaration, so collapse dots to hyphens.
     */
    private static String cssSafe(String name) {
        return name.replace('.', '-');
    }

    static String serialize(List<Declaration> declarations, String prefix) {
        var variablePrefix = prefix == null || prefix.isEmpty() ? "" : prefix + "-";
        if (declarations.isEmpty()) {
            return ":root {\n}\n";
        }
        var lines = declarations.stream()
                .map(declaration -> "  --%s%s: %s;".formatted(variablePrefix, declaration.name(), declaration.value()))
                .toList();
        return ":root {\n" + String.join("\n", lines) + "\n}\n";
    }
}

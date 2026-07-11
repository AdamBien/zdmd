package airhacks.zdmd.parsing.entity;

import java.util.List;
import java.util.Map;

/**
 * Raw, unresolved parse output — mirrors the DESIGN.md YAML schema.
 * Map-valued fields are null when the corresponding top-level key is absent
 * or not a mapping. {@code rawValues} holds every merged top-level key,
 * known and unknown.
 */
public record ParsedDesignSystem(
        String version,
        String name,
        String description,
        Map<String, Object> colors,
        Map<String, Object> typography,
        Map<String, Object> rounded,
        Map<String, Object> spacing,
        Map<String, Object> components,
        Map<String, SourceLocation> sourceMap,
        List<String> sections,
        List<DocumentSection> documentSections,
        Map<String, Object> rawValues) {

    public static ParsedDesignSystem empty() {
        return new ParsedDesignSystem(null, null, null, null, null, null, null, null,
                Map.of(), List.of(), List.of(), Map.of());
    }
}

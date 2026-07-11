package airhacks.zdmd.parsing.boundary;

import airhacks.zdmd.parsing.control.MarkdownScanner;
import airhacks.zdmd.parsing.control.Yaml;
import airhacks.zdmd.parsing.entity.ParseResult;
import airhacks.zdmd.parsing.entity.ParsedDesignSystem;
import airhacks.zdmd.parsing.entity.SourceLocation;
import airhacks.zdmd.parsing.entity.YamlBlock;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Extracts and parses YAML design tokens from DESIGN.md content.
 * Supports two embedding modes: frontmatter (---) and fenced yaml code
 * blocks. Never throws — all errors are returned as ParseResult failures.
 */
public interface DesignMdParser {

    static ParseResult parse(String content) {
        var scanned = MarkdownScanner.scan(content);

        if (scanned.blocks().isEmpty()) {
            return new ParseResult.Failure("NO_YAML_FOUND",
                    "No YAML content found. Expected frontmatter (---) or fenced yaml code blocks.", true);
        }

        var merged = new LinkedHashMap<String, Object>();
        var sourceMap = new LinkedHashMap<String, SourceLocation>();
        var seenSections = new LinkedHashMap<String, String>();

        for (var block : scanned.blocks()) {
            Object parsed;
            try {
                parsed = Yaml.parse(block.yaml());
            } catch (Yaml.YamlException error) {
                return new ParseResult.Failure("YAML_PARSE_ERROR", error.getMessage(), true);
            }
            if (!(parsed instanceof Map<?, ?> parsedMap)) {
                continue;
            }
            for (var key : parsedMap.keySet()) {
                var name = String.valueOf(key);
                var previousBlock = seenSections.get(name);
                if (previousBlock != null) {
                    return new ParseResult.Failure("DUPLICATE_SECTION",
                            "Section '%s' is defined in both %s and %s.".formatted(name, previousBlock, block.label()),
                            true);
                }
                seenSections.put(name, block.label());
                sourceMap.put(name, new SourceLocation(block.startLine(), 0, block.label()));
            }
            parsedMap.forEach((key, value) -> merged.put(String.valueOf(key), value));
        }

        return new ParseResult.Success(toDesignSystem(merged, sourceMap, scanned));
    }

    private static ParsedDesignSystem toDesignSystem(Map<String, Object> raw,
            Map<String, SourceLocation> sourceMap,
            airhacks.zdmd.parsing.entity.ScannedDocument scanned) {
        return new ParsedDesignSystem(
                stringOrNull(raw.get("version")),
                stringOrNull(raw.get("name")),
                stringOrNull(raw.get("description")),
                mapOrNull(raw.get("colors")),
                mapOrNull(raw.get("typography")),
                mapOrNull(raw.get("rounded")),
                mapOrNull(raw.get("spacing")),
                mapOrNull(raw.get("components")),
                sourceMap,
                scanned.sectionNames(),
                scanned.documentSections(),
                raw);
    }

    private static String stringOrNull(Object value) {
        return value instanceof String s ? s : null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapOrNull(Object value) {
        return value instanceof Map<?, ?> ? (Map<String, Object>) value : null;
    }
}

package airhacks.zdmd.reporting.boundary;

import java.util.List;
import java.util.Map;

/**
 * Human-readable markdown rendering of report structures — the
 * `--format md` output. Lint reports get a dedicated layout; other
 * structures fall back to an indented key/value listing.
 */
public interface Markdown {

    static String format(Object data) {
        if (!(data instanceof Map<?, ?> map)) {
            return String.valueOf(data);
        }
        if (isLintOutput(map)) {
            return formatLint(map);
        }
        var result = new StringBuilder();
        if (map.get("summary") instanceof String summary) {
            result.append("# ").append(summary).append("\n\n");
        }
        var details = map.get("details");
        if (details != null) {
            result.append("## Details\n\n").append(asText(details, 0)).append('\n');
        }
        if (map.get("patches") instanceof List<?> patches && !patches.isEmpty()) {
            result.append("## Patches\n\n").append(asText(patches, 0)).append('\n');
        }
        return result.isEmpty() ? asText(data, 0) : result.toString();
    }

    private static boolean isLintOutput(Map<?, ?> map) {
        return map.get("findings") instanceof List
                && map.get("summary") instanceof Map<?, ?> summary
                && summary.get("errors") instanceof Number
                && summary.get("warnings") instanceof Number
                && summary.get("infos") instanceof Number;
    }

    private static String formatLint(Map<?, ?> map) {
        var summary = (Map<?, ?>) map.get("summary");
        var findings = (List<?>) map.get("findings");

        var result = new StringBuilder("# Lint Report\n\n");
        result.append("**%s errors**, **%s warnings**, **%s infos**\n"
                .formatted(summary.get("errors"), summary.get("warnings"), summary.get("infos")));

        if (!findings.isEmpty()) {
            result.append("\n## Findings\n\n");
            for (var finding : findings) {
                if (finding instanceof Map<?, ?> f) {
                    var location = f.get("path") instanceof String path ? " `" + path + "`:" : ":";
                    result.append("- **%s**%s %s\n".formatted(f.get("severity"), location, f.get("message")));
                }
            }
        }
        return result.toString();
    }

    private static String asText(Object data, int indent) {
        return switch (data) {
            case null -> "null";
            case String text -> text;
            case Number number -> Json.number(number);
            case Boolean bool -> String.valueOf(bool);
            case List<?> list -> list.stream()
                    .map(item -> "  ".repeat(indent) + "- " + asText(item, indent + 1))
                    .reduce((a, b) -> a + "\n" + b).orElse("");
            case Map<?, ?> map -> map.entrySet().stream()
                    .map(entry -> {
                        var value = entry.getValue();
                        var rendered = value instanceof Map<?, ?> || value instanceof List<?>
                                ? "\n" + asText(value, indent + 1)
                                : " " + asText(value, indent + 1);
                        return "  ".repeat(indent) + entry.getKey() + ":" + rendered;
                    })
                    .reduce((a, b) -> a + "\n" + b).orElse("");
            default -> String.valueOf(data);
        };
    }
}

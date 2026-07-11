import airhacks.zdmd.reporting.boundary.Markdown;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// covers reporting R2.1
void main() {
    var finding = new LinkedHashMap<String, Object>();
    finding.put("severity", "error");
    finding.put("path", "colors.broken");
    finding.put("message", "'nope' is not a valid color.");

    var summary = new LinkedHashMap<String, Object>();
    summary.put("errors", 1);
    summary.put("warnings", 0);
    summary.put("infos", 0);

    var output = Markdown.format(Map.of("findings", List.of(finding), "summary", summary));

    assert output.startsWith("# Lint Report") : "report title, got: " + output;
    assert output.contains("**1 errors**, **0 warnings**, **0 infos**") : "bold counts, got: " + output;
    assert output.contains("- **error** `colors.broken`: 'nope' is not a valid color.")
            : "finding bullet, got: " + output;
}

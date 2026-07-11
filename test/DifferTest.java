import airhacks.zdmd.diffing.boundary.Differ;
import airhacks.zdmd.linting.boundary.Linter;

import java.util.List;
import java.util.Map;

// covers diffing R1.1, diffing R1.2, diffing R1.3
void main() {
    var before = Linter.lint("""
            ```yaml
            colors:
              primary: "#111111"
              removed-soon: "#222222"
            ```
            """);
    var after = Linter.lint("""
            ```yaml
            colors:
              primary: "#333333"
              fresh: "#444444"
            ```
            """);

    var diff = Differ.diff(before, after);
    var tokens = (Map<?, ?>) diff.get("tokens");
    var colors = (Map<?, ?>) tokens.get("colors");

    assert List.of("fresh").equals(colors.get("added")) : "added: got " + colors.get("added");
    assert List.of("removed-soon").equals(colors.get("removed")) : "removed: got " + colors.get("removed");
    assert List.of("primary").equals(colors.get("modified")) : "modified: got " + colors.get("modified");
    assert !Differ.regression(diff) : "same finding counts is no regression";

    var broken = Linter.lint("""
            ```yaml
            colors:
              primary: oops
            ```
            """);
    var regression = Differ.diff(before, broken);
    assert Differ.regression(regression) : "new errors flag a regression";
    var findings = (Map<?, ?>) regression.get("findings");
    var delta = (Map<?, ?>) findings.get("delta");
    assert Integer.valueOf(1).equals(delta.get("errors")) : "error delta: got " + delta.get("errors");
}

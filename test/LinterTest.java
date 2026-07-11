import airhacks.zdmd.linting.boundary.Linter;
import airhacks.zdmd.tokens.entity.Severity;

void main() {
    resolvesTokensAndReferences();
    reportsErrors();
    recoversWithoutYaml();
    detectsLowContrast();
}

// covers tokens R2.1
void resolvesTokensAndReferences() {
    var report = Linter.lint("""
            ## Colors

            ```yaml
            colors:
              primary: "#1A1C1E"
              alias: "{colors.primary}"
            components:
              button:
                backgroundColor: "{colors.primary}"
            ```
            """);
    assert report.summary().errors() == 0 : "expected no errors but got " + report.findings();
    assert report.designSystem().colors().containsKey("alias") : "chained reference resolves";
    assert "#1a1c1e".equals(report.designSystem().colors().get("alias").hex().toLowerCase())
            : "alias points at primary: got " + report.designSystem().colors().get("alias").hex();
}

// covers tokens R1.2, linting R1.1
void reportsErrors() {
    var report = Linter.lint("""
            ```yaml
            colors:
              broken: nope
            ```
            """);
    assert report.summary().errors() == 1 : "expected 1 error but got " + report.summary().errors();
    var finding = report.findings().getFirst();
    assert finding.severity() == Severity.ERROR : "severity: got " + finding.severity();
    assert "colors.broken".equals(finding.path()) : "path: got " + finding.path();
}

// covers linting R1.2
void recoversWithoutYaml() {
    var report = Linter.lint("# Just prose\n\n## Colors\n\nno tokens\n");
    assert report.summary().errors() == 0 : "no yaml is not an error";
    assert report.summary().warnings() == 1 : "no yaml yields exactly one warning, got " + report.summary().warnings();
    assert report.findings().getFirst().message().contains("No YAML content found")
            : "warning message: got " + report.findings().getFirst().message();
    assert report.sections().contains("Colors") : "sections extracted from raw markdown: " + report.sections();
}

// covers linting R2.1
void detectsLowContrast() {
    var report = Linter.lint("""
            ```yaml
            colors:
              primary: "#777777"
            components:
              card:
                backgroundColor: "#777777"
                textColor: "#888888"
            ```
            """);
    var contrastWarning = report.findings().stream()
            .anyMatch(f -> f.message().contains("below WCAG AA minimum"));
    assert contrastWarning : "expected a contrast warning in " + report.findings();
}

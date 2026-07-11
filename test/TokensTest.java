import airhacks.zdmd.linting.boundary.Linter;
import airhacks.zdmd.tokens.entity.Severity;

void main() {
    unresolvedAndCircularReferences();
    dimensionUnits();
    unitlessLineHeight();
}

// covers tokens R2.2, linting R2.4
void unresolvedAndCircularReferences() {
    var report = Linter.lint("""
            ```yaml
            colors:
              a: "{colors.b}"
              b: "{colors.a}"
            components:
              button:
                backgroundColor: "{colors.missing}"
            ```
            """);
    assert !report.designSystem().colors().containsKey("a") : "circular references stay unresolved";
    var button = report.designSystem().components().get("button");
    assert button.unresolvedRefs().contains("{colors.missing}")
            : "unresolved component reference recorded, got " + button.unresolvedRefs();
    var brokenRef = report.findings().stream().anyMatch(f -> f.severity() == Severity.ERROR
            && f.message().contains("{colors.missing}") && f.message().contains("does not resolve"));
    assert brokenRef : "broken reference reported as error in " + report.findings();
}

// covers tokens R3.1, tokens R3.2
void dimensionUnits() {
    var report = Linter.lint("""
            ```yaml
            spacing:
              fluid: 2cqi
            rounded:
              weird: 2vw
            ```
            """);
    assert report.designSystem().spacing().containsKey("fluid") : "container-query unit parses";
    assert "cqi".equals(report.designSystem().spacing().get("fluid").unit())
            : "unit kept: got " + report.designSystem().spacing().get("fluid").unit();
    var unitError = report.findings().stream().anyMatch(f -> "rounded.weird".equals(f.path())
            && f.message().contains("invalid unit 'vw'"));
    assert unitError : "non-standard rounded unit flagged in " + report.findings();
    assert report.designSystem().rounded().containsKey("weird") : "flagged dimension still kept";
}

// covers tokens R3.3
void unitlessLineHeight() {
    var report = Linter.lint("""
            ```yaml
            typography:
              body:
                fontSize: 16px
                lineHeight: "1.6"
            ```
            """);
    var body = report.designSystem().typography().get("body");
    assert body.lineHeight() != null && body.lineHeight().value() == 1.6 && body.lineHeight().unit().isEmpty()
            : "quoted unitless line height becomes multiplier, got " + body.lineHeight();
}

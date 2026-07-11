import airhacks.zdmd.exporting.boundary.Exporter;
import airhacks.zdmd.linting.boundary.Linter;

void main() {
    var report = Linter.lint("""
            ```yaml
            name: Demo
            colors:
              primary: "#1A1C1E"
              nested:
                light: "#FFFFFF"
            spacing:
              sm: 4px
            rounded:
              md: 0.75rem
            typography:
              h1:
                fontFamily: Public Sans
                fontSize: 48px
                fontWeight: 600
                lineHeight: "1.1"
            ```
            """);
    cssVars(report);
    dtcg(report);
    formats();
}

// covers exporting R1.1, exporting R1.2, exporting R1.3
void cssVars(airhacks.zdmd.linting.entity.LintReport report) {
    var css = Exporter.export(report.designSystem(), "css-vars", null);
    assert css.startsWith(":root {") : "css-vars starts with :root, got " + css.lines().findFirst().orElse("");
    assert css.contains("--color-primary: #1a1c1e;") : "hex lowercased: " + css;
    assert css.contains("--color-nested-light: #ffffff;") : "nested token dots become hyphens: " + css;
    assert css.contains("--spacing-sm: 4px;") : "integral dimension without decimal point: " + css;
    assert css.contains("--rounded-md: 0.75rem;") : "fractional dimension kept: " + css;

    var prefixed = Exporter.export(report.designSystem(), "css-vars", "zd");
    assert prefixed.contains("--zd-color-primary:") : "prefix applied: " + prefixed;
}

// covers exporting R2.1, exporting R2.2, exporting R2.3, tokens R3.3
void dtcg(airhacks.zdmd.linting.entity.LintReport report) {
    var json = Exporter.export(report.designSystem(), "dtcg", null);
    assert json.contains("\"$schema\": \"https://www.designtokens.org/schemas/2025.10/format.json\"")
            : "schema url present";
    assert json.contains("\"$description\": \"Demo\"") : "name becomes description: " + json;
    assert json.contains("\"colorSpace\": \"srgb\"") : "srgb color space";
    assert json.contains("\"hex\": \"#1a1c1e\"") : "hex included: " + json;
    assert json.contains("\"fontWeight\": 600") : "integral font weight without decimal: " + json;
    assert json.contains("\"lineHeight\": 1.1") : "unitless line height: " + json;
    assert json.contains("\"value\": 4,") : "dimension value as number: " + json;
}

// covers exporting R3.1
void formats() {
    assert Exporter.supports("css-vars") : "css-vars supported";
    assert Exporter.supports("dtcg") : "dtcg supported";
    assert !Exporter.supports("tailwind") : "tailwind deliberately unsupported";
}

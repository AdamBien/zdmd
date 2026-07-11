import airhacks.zdmd.linting.boundary.Linter;

void main() {
    typoHint();
    tokenLikeIgnored();
}

// covers linting R2.2
void typoHint() {
    var report = Linter.lint("""
            ```yaml
            colours:
              primary: "#ff0000"
            ```
            """);
    var hint = report.findings().stream().anyMatch(f -> "colours".equals(f.path())
            && f.message().contains("did you mean \"colors\""));
    assert hint : "typo hint expected in " + report.findings();
}

// covers linting R2.3
void tokenLikeIgnored() {
    var report = Linter.lint("""
            ```yaml
            metrics:
              card:
                width: 320px
            ```
            """);
    var ignored = report.findings().stream().anyMatch(f -> "metrics".equals(f.path())
            && f.message().contains("silently ignored by export commands"));
    assert ignored : "token-like unknown key warning expected in " + report.findings();
}

import airhacks.zdmd.parsing.boundary.DesignMdParser;
import airhacks.zdmd.parsing.entity.ParseResult;

void main() {
    noYamlFound();
    yamlParseError();
    duplicateSection();
}

// covers parsing R1.3
void noYamlFound() {
    var result = DesignMdParser.parse("# Just prose\n\nno tokens here\n");
    assert result instanceof ParseResult.Failure failure
            && "NO_YAML_FOUND".equals(failure.code())
            && failure.recoverable() : "expected recoverable NO_YAML_FOUND but got " + result;
}

// covers parsing R1.4
void yamlParseError() {
    var result = DesignMdParser.parse("""
            ```yaml
            key: 1
            key: 2
            ```
            """);
    assert result instanceof ParseResult.Failure failure
            && "YAML_PARSE_ERROR".equals(failure.code())
            && failure.recoverable() : "expected recoverable YAML_PARSE_ERROR but got " + result;
}

// covers parsing R1.5
void duplicateSection() {
    var result = DesignMdParser.parse("""
            ---
            colors:
              primary: "#111111"
            ---

            ```yaml
            colors:
              secondary: "#222222"
            ```
            """);
    assert result instanceof ParseResult.Failure failure
            && "DUPLICATE_SECTION".equals(failure.code())
            && failure.message().contains("frontmatter")
            && failure.message().contains("code block 1")
            : "expected DUPLICATE_SECTION naming both blocks but got " + result;
}

package airhacks.zdmd.parsing.entity;

/**
 * Outcome of parsing a DESIGN.md document. Recoverable failures (no YAML,
 * broken YAML, duplicate sections) let the linter degrade to a warning
 * instead of aborting.
 */
public sealed interface ParseResult {

    record Success(ParsedDesignSystem data) implements ParseResult {
    }

    record Failure(String code, String message, boolean recoverable) implements ParseResult {
    }
}

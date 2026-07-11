package airhacks.zdmd.parsing.entity;

/**
 * Where a top-level YAML key was defined. {@code block} is "frontmatter"
 * or "code block N" (1-based).
 */
public record SourceLocation(int line, int column, String block) {
}

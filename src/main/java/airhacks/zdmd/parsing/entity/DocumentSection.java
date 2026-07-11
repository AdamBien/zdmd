package airhacks.zdmd.parsing.entity;

/**
 * A slice of the document delimited by H2 headings. The prelude before the
 * first heading has an empty heading.
 */
public record DocumentSection(String heading, String content) {
}

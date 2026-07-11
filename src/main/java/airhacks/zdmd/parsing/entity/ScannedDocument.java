package airhacks.zdmd.parsing.entity;

import java.util.List;

/**
 * Result of the markdown scan: all YAML blocks, the H2 headings with their
 * 1-based line numbers, and the document partitioned into sections.
 */
public record ScannedDocument(List<YamlBlock> blocks, List<Heading> headings, List<DocumentSection> documentSections) {

    public record Heading(String text, int line) {
    }

    public List<String> sectionNames() {
        return this.headings.stream().map(Heading::text).toList();
    }
}

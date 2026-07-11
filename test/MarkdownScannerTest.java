import airhacks.zdmd.parsing.control.MarkdownScanner;

void main() {
    frontmatterAndFences();
    headingsInsideFencesIgnored();
    sections();
}

// covers parsing R1.1, parsing R1.2
void frontmatterAndFences() {
    var scanned = MarkdownScanner.scan("""
            ---
            name: Demo
            ---

            # Title

            ```yaml
            colors:
              primary: "#fff"
            ```

            ```js
            let ignored = true;
            ```
            """);
    assert scanned.blocks().size() == 2 : "expected 2 yaml blocks but got " + scanned.blocks().size();
    var frontmatter = scanned.blocks().getFirst();
    assert frontmatter.frontmatter() : "first block must be frontmatter";
    assert frontmatter.startLine() == 1 : "frontmatter starts at line 1, got " + frontmatter.startLine();
    assert "name: Demo".equals(frontmatter.yaml()) : "frontmatter yaml: got " + frontmatter.yaml();
    var fenced = scanned.blocks().get(1);
    assert "code block 1".equals(fenced.label()) : "label: got " + fenced.label();
    assert fenced.yaml().contains("primary") : "fenced yaml content: got " + fenced.yaml();
}

// covers parsing R2.1
void headingsInsideFencesIgnored() {
    var scanned = MarkdownScanner.scan("""
            ## Real Heading

            ```yaml
            text: |
              ## not a heading
            ```
            """);
    assert scanned.headings().size() == 1 : "expected 1 heading but got " + scanned.headings().size();
    assert "Real Heading".equals(scanned.headings().getFirst().text())
            : "heading text: got " + scanned.headings().getFirst().text();
}

// covers parsing R2.2
void sections() {
    var scanned = MarkdownScanner.scan("""
            prelude

            ## Colors

            body

            ## Typography ##

            more
            """);
    assert scanned.sectionNames().equals(java.util.List.of("Colors", "Typography"))
            : "sections: got " + scanned.sectionNames();
    var documentSections = scanned.documentSections();
    assert documentSections.size() == 3 : "expected prelude + 2 sections but got " + documentSections.size();
    assert documentSections.getFirst().heading().isEmpty() : "prelude has empty heading";
    assert documentSections.get(1).content().contains("body") : "Colors section holds its body";
}

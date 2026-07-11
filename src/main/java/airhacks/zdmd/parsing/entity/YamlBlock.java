package airhacks.zdmd.parsing.entity;

/**
 * A YAML source block found in a DESIGN.md document — either the frontmatter
 * or a fenced {@code ```yaml} code block. {@code index} is the zero-based
 * position among fenced blocks; frontmatter has index -1.
 */
public record YamlBlock(String yaml, boolean frontmatter, int index, int startLine) {

    public String label() {
        return frontmatter ? "frontmatter" : "code block " + (index + 1);
    }
}

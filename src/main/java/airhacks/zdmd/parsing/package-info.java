/// # parsing
///
/// > Extracts YAML design-token blocks and document structure from DESIGN.md markdown.
///
/// ## Boundary
///
/// - `parse-design-md` — turn raw DESIGN.md content into an unresolved design-system
///   structure, or a recoverable failure.
///
/// ## Requirements
///
/// ### R1 — YAML extraction
///
/// - R1.1 When the document starts with a `---` frontmatter block, the BC shall extract it as a YAML block starting at line 1.
/// - R1.2 When the document contains fenced `yaml`/`yml` code blocks, the BC shall extract each with its opening-fence line number, in document order.
/// - R1.3 If no YAML content is found, then the BC shall fail recoverably with `NO_YAML_FOUND`.
/// - R1.4 If a YAML block cannot be parsed, then the BC shall fail recoverably with `YAML_PARSE_ERROR`.
/// - R1.5 If the same top-level key is defined in two blocks, then the BC shall fail recoverably with `DUPLICATE_SECTION`, naming both blocks.
///
/// ### R2 — Document structure
///
/// - R2.1 The BC shall collect H2 headings as section names, ignoring headings inside code fences.
/// - R2.2 The BC shall partition the document into sections along H2 heading lines, with a heading-less prelude before the first.
///
/// ### R3 — YAML subset
///
/// - R3.1 The BC shall parse nested block mappings and sequences, quoted and plain scalars (typed as boolean, integer, float, null, or string), single-line flow collections, literal and folded block scalars, and comments.
/// - R3.2 If a document uses anchors, aliases, tags, tab indentation, duplicate mapping keys, or multiple documents, then the BC shall reject it. _(why: zero-dependency subset — reject loudly instead of guessing)_
///
/// ## Entities
///
/// - `yaml-block` — YAML source with origin (frontmatter or code-block index) and start line.
/// - `document-section` — an H2-delimited slice of the document.
/// - `parsed-design-system` — raw top-level keys mirroring the DESIGN.md schema, plus a source map.
///
/// ## Out of scope
///
/// - Setext headings, fences indented four or more spaces, inline markdown in heading text.
/// - Resolving token values or references — owned by `tokens`.
package airhacks.zdmd.parsing;

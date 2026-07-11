/// # reporting
///
/// > Renders report structures as machine-readable JSON and human-readable
/// > markdown.
///
/// ## Boundary
///
/// - `render-json` — pretty or compact JSON from maps, lists, and scalars.
/// - `render-markdown` — markdown rendering of report structures.
///
/// ## Requirements
///
/// ### R1 — JSON
///
/// - R1.1 The BC shall render pretty JSON with two-space indentation, insertion order preserved, and integral doubles without a decimal point. _(why: byte parity with JSON.stringify(x, null, 2))_
/// - R1.2 The BC shall escape quotes, backslashes, and control characters per JSON.
/// - R1.3 The BC shall render compact JSON without whitespace.
///
/// ### R2 — Markdown
///
/// - R2.1 When a structure carries findings and severity counts, the BC shall render a Lint Report with bold counts and one bullet per finding.
///
/// ## Out of scope
///
/// - Deciding report content — callers assemble the structures.
package airhacks.zdmd.reporting;

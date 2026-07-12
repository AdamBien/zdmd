/// # exporting
///
/// > Exports a resolved design system to web-standard formats: CSS custom
/// > properties and W3C Design Tokens (DTCG) JSON.
///
/// ## Boundary
///
/// - `export-tokens` — serialize a design system in a requested format, with an optional CSS variable prefix.
/// - `validate-format` — decide whether a format name is supported.
/// - `write-tokens` — persist a serialized export to its conventional filename in the working directory.
///
/// ## Requirements
///
/// ### R1 — CSS custom properties
///
/// - R1.1 The BC shall emit a `:root` block with `--color-*`, `--spacing-*`, then `--rounded-*` declarations, hex values lowercased and integral numbers without a decimal point.
/// - R1.2 The BC shall collapse dots in nested token names to hyphens. _(why: a literal dot makes browsers drop the declaration)_
/// - R1.3 Where a prefix is provided, the BC shall prepend `--<prefix>-` to every variable name.
///
/// ### R2 — DTCG
///
/// - R2.1 The BC shall emit a DTCG 2025.10 tokens.json with `$schema`, sRGB components rounded to three decimals, and the hex fallback.
/// - R2.2 The BC shall emit typography tokens with font family, size, weight, letter spacing, and unitless line-height multipliers.
/// - R2.3 Where a name or description is present, the BC shall emit `$description`, preferring the description.
///
/// ### R3 — Format validation
///
/// - R3.1 If the requested format is not `css-vars` or `dtcg`, then the BC shall reject it.
///
/// ### R4 — File materialization
///
/// - R4.1 When a serialized export is written, the BC shall place `dtcg` output in `tokens.json` and `css-vars` output in `tokens.css` in the working directory.
/// - R4.2 If the target file already exists, then the BC shall overwrite it. _(why: exports are generated artifacts — regenerating is the point)_
/// - R4.3 If the target file cannot be written, then the BC shall fail, naming the file and the reason.
///
/// ## Out of scope
///
/// - Tailwind v3/v4 formats — deliberately not ported; web standards only.
package airhacks.zdmd.exporting;

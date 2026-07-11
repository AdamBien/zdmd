/// # linting
///
/// > Lints a DESIGN.md document: parses it, resolves the token model, and runs
/// > the default rule set into a findings report.
///
/// ## Boundary
///
/// - `lint` — produce a lint report (design system, findings, severity summary, sections) from raw DESIGN.md content.
///
/// ## Requirements
///
/// ### R1 — Report assembly
///
/// - R1.1 When linting a document, the BC shall combine model findings with the default rules in stable order and count errors, warnings, and infos.
/// - R1.2 If parsing fails recoverably, then the BC shall return an empty design system with exactly one warning and sections extracted from the raw markdown. _(why: agents keep working on prose-only documents)_
///
/// ### R2 — Rules
///
/// - R2.1 If a component's textColor on backgroundColor contrast is below 4.5:1, then the BC shall warn, citing both hex values and the ratio.
/// - R2.2 If an unknown top-level key is within edit distance 2 of a schema key, then the BC shall warn with a "did you mean" hint.
/// - R2.3 If an unknown top-level key holds a design-token-like map, then the BC shall warn that export silently ignores it.
/// - R2.4 If a component reference does not resolve, then the BC shall report an error naming the reference.
///
/// ## Entities
///
/// - `lint-report` — design system, findings, summary, document sections.
/// - `summary` — error/warning/info counts.
///
/// ## Out of scope
///
/// - Auto-fixing findings; output rendering (owned by `reporting`).
package airhacks.zdmd.linting;

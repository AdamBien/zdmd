/// # tokens
///
/// > Resolves raw parsed values into a typed design system: colors, dimensions,
/// > typography, components, and chained token references.
///
/// ## Boundary
///
/// - `resolve-tokens` — build the resolved design system and model findings from a parsed document.
/// - `contrast-ratio` — WCAG 2.1 contrast ratio between two resolved colors.
///
/// ## Requirements
///
/// ### R1 — Colors
///
/// - R1.1 The BC shall parse hex, named, rgb/hsl/hwb/lab/lch/oklab/oklch, and `color-mix(in srgb, …)` color values into sRGB with WCAG relative luminance.
/// - R1.2 If a color value is invalid, then the BC shall emit an error finding at `colors.<token>` and keep the raw value in the symbol table.
///
/// ### R2 — References
///
/// - R2.1 When a token value is `{section.token}`, the BC shall resolve it through chained references.
/// - R2.2 If a reference is circular, unknown, or nested deeper than 10 hops, then the BC shall leave it unresolved; component references are recorded as unresolved.
///
/// ### R3 — Dimensions & typography
///
/// - R3.1 The BC shall parse `<number><unit>` dimension values for all known CSS units.
/// - R3.2 If a rounded or typography dimension uses a unit other than px, rem, or em, then the BC shall emit an error finding while keeping the parsed value. _(why: upstream parity — flagged but exported)_
/// - R3.3 While a `lineHeight` is a quoted unitless number, the BC shall treat it as a multiplier with an empty unit.
///
/// ## Entities
///
/// - `resolved-color` — sRGB channels, hex, optional alpha, WCAG luminance.
/// - `resolved-dimension` — numeric value plus unit.
/// - `resolved-typography` — optional font family, size, weight, line height, letter spacing, features.
/// - `component-def` — resolved properties plus unresolved references.
/// - `design-system` — all token maps, the flat symbol table, sections, unknown keys.
/// - `finding` — severity, optional token path, message.
///
/// ## Out of scope
///
/// - Lint policy (owned by `linting`) and output formats (owned by `exporting`).
package airhacks.zdmd.tokens;

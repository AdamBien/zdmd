/// # diffing
///
/// > Compares two lint reports: token-level changes per section and
/// > finding-count regressions.
///
/// ## Boundary
///
/// - `diff-design-systems` — produce the change structure between a before and an after lint report.
/// - `flag-regression` — read the regression verdict off a diff result.
///
/// ## Requirements
///
/// ### R1 — Token and finding deltas
///
/// - R1.1 The BC shall report added, removed, and modified token keys per section (colors, typography, rounded, spacing, components).
/// - R1.2 The BC shall report before and after severity summaries plus their error/warning delta.
/// - R1.3 The BC shall flag a regression when errors or warnings increased.
///
/// ## Out of scope
///
/// - Value-level diffs (only key names are reported) and rendering.
package airhacks.zdmd.diffing;

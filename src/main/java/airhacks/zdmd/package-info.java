/// # zdmd
///
/// **Charter**: zdmd lints, diffs, and exports DESIGN.md design tokens as a
/// zero-dependency Java CLI.
///
/// **Vision**: Design tokens verified and exported anywhere Java runs, with
/// zero dependencies.
///
/// ## Components
///
/// - `linting` calls `parsing` (extract YAML) and `tokens` (resolve the model).
/// - `diffing` consumes `linting` reports.
/// - `exporting` consumes `tokens` design systems and renders JSON via `reporting`.
/// - The CLI shell (`airhacks.App`) orchestrates all boundaries and owns argument
///   parsing, input reading, and exit codes.
///
/// ## System invariants
///
/// - S1.1 When a command succeeds, the system shall write results to stdout and exit 0.
/// - S1.2 If input cannot be read, then the system shall write a friendly error to stderr and exit 2.
/// - S1.3 If lint finds errors or diff finds a regression, then the system shall exit 1.
/// - S1.4 When an export succeeds, the system shall exit 0 regardless of lint findings in the source. _(why: findings are `lint`'s concern; exports must stay scriptable)_
/// - S1.5 When an export succeeds, the system shall also write the serialized output to its conventional file in the working directory. _(why: agents want files without shell redirection; stdout stays intact for pipes)_
///
/// ## Ubiquitous language
///
/// - **design system** — the resolved token model of one DESIGN.md document.
/// - **token** — a named design value (color, dimension, typography) addressable as `section.name`.
/// - **finding** — one diagnostic with severity error/warning/info.
/// - **regression** — an increase in errors or warnings between two documents.
///
/// ## Stack
///
/// Stack: `/java-cli-app` + `/bce`, built with `/zb`, tested with `/zunit`; package base `airhacks.zdmd`.
/// Trace convention: tests mark coverage with a `// covers <bc> Rn.m` (or `// covers S1.n`) comment.
package airhacks.zdmd;

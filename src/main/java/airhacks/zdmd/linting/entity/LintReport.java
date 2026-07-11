package airhacks.zdmd.linting.entity;

import airhacks.zdmd.parsing.entity.DocumentSection;
import airhacks.zdmd.tokens.entity.DesignSystem;
import airhacks.zdmd.tokens.entity.Finding;

import java.util.List;

/**
 * Result of linting a DESIGN.md document: the resolved design system,
 * all findings, severity counts, and the document's H2 sections.
 */
public record LintReport(
        DesignSystem designSystem,
        List<Finding> findings,
        Summary summary,
        List<String> sections,
        List<DocumentSection> documentSections) {
}

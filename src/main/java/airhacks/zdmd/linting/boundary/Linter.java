package airhacks.zdmd.linting.boundary;

import airhacks.zdmd.linting.control.Rules;
import airhacks.zdmd.linting.entity.LintReport;
import airhacks.zdmd.linting.entity.Summary;
import airhacks.zdmd.parsing.boundary.DesignMdParser;
import airhacks.zdmd.parsing.entity.DocumentSection;
import airhacks.zdmd.parsing.entity.ParseResult;
import airhacks.zdmd.parsing.entity.ParsedDesignSystem;
import airhacks.zdmd.tokens.boundary.TokenModel;
import airhacks.zdmd.tokens.entity.Finding;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Lint a DESIGN.md document: parse the markdown, resolve all design tokens
 * into a typed model, and run the lint rules. Recoverable parse failures
 * degrade to a warning with an empty design system; unrecoverable ones throw.
 */
public interface Linter {

    static LintReport lint(String content) {
        var parseResult = DesignMdParser.parse(content);

        return switch (parseResult) {
            case ParseResult.Failure failure when failure.recoverable() -> recovered(content, failure);
            case ParseResult.Failure failure -> throw new IllegalStateException("Parse failed: " + failure.message());
            case ParseResult.Success(ParsedDesignSystem parsed) -> {
                var model = TokenModel.resolve(parsed);
                var findings = new ArrayList<>(model.findings());
                findings.addAll(Rules.run(model.designSystem()));
                yield new LintReport(model.designSystem(), findings, Summary.of(findings),
                        parsed.sections(), parsed.documentSections());
            }
        };
    }

    private static LintReport recovered(String content, ParseResult.Failure failure) {
        var model = TokenModel.resolve(ParsedDesignSystem.empty());
        var sections = extractSectionsFromContent(content);
        var findings = List.of(new Finding(airhacks.zdmd.tokens.entity.Severity.WARNING, null, failure.message()));
        var sectionNames = sections.stream().map(DocumentSection::heading).filter(h -> !h.isEmpty()).toList();
        return new LintReport(model.designSystem(), findings, new Summary(0, 1, 0), sectionNames, sections);
    }

    Pattern HEADING = Pattern.compile("^## (.+)$");

    /**
     * Fallback section extraction from raw markdown when no YAML could be
     * parsed. Fence-unaware by design — mirrors the upstream fallback.
     */
    private static List<DocumentSection> extractSectionsFromContent(String content) {
        var lines = content.split("\n", -1);
        var sections = new ArrayList<DocumentSection>();
        var all = List.of(lines);

        var currentStart = 0;
        var currentHeading = "";

        for (var i = 0; i < lines.length; i++) {
            if (lines[i].isEmpty()) {
                continue;
            }
            var matcher = HEADING.matcher(lines[i]);
            if (matcher.matches()) {
                if (i > 0) {
                    sections.add(new DocumentSection(currentHeading, String.join("\n", all.subList(currentStart, i))));
                }
                currentHeading = matcher.group(1);
                currentStart = i;
            }
        }
        sections.add(new DocumentSection(currentHeading, String.join("\n", all.subList(currentStart, lines.length))));
        return sections;
    }
}

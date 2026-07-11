package airhacks.zdmd.linting.entity;

import airhacks.zdmd.tokens.entity.Finding;
import airhacks.zdmd.tokens.entity.Severity;

import java.util.List;

/** Aggregate finding counts by severity. */
public record Summary(int errors, int warnings, int infos) {

    public static Summary of(List<Finding> findings) {
        return new Summary(
                count(findings, Severity.ERROR),
                count(findings, Severity.WARNING),
                count(findings, Severity.INFO));
    }

    static int count(List<Finding> findings, Severity severity) {
        return (int) findings.stream().filter(f -> f.severity() == severity).count();
    }
}

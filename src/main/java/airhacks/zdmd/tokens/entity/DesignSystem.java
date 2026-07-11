package airhacks.zdmd.tokens.entity;

import java.util.List;
import java.util.Map;

/**
 * The fully resolved design system model. The symbol table is a flat
 * lookup ("colors.primary" → ResolvedColor) that also holds raw values
 * for tokens that failed to resolve.
 */
public record DesignSystem(
        String name,
        String description,
        Map<String, ResolvedColor> colors,
        Map<String, ResolvedTypography> typography,
        Map<String, ResolvedDimension> rounded,
        Map<String, ResolvedDimension> spacing,
        Map<String, ComponentDef> components,
        Map<String, Object> symbolTable,
        List<String> sections,
        List<String> unknownKeys,
        Map<String, Object> unknownKeyValues) {
}

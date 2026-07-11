package airhacks.zdmd.tokens.entity;

import java.util.List;
import java.util.Map;

/**
 * A component's resolved properties. Values are ResolvedColor,
 * ResolvedDimension, ResolvedTypography, String, Number, or Boolean.
 * References that failed to resolve are listed in {@code unresolvedRefs}
 * and kept verbatim in {@code properties}.
 */
public record ComponentDef(Map<String, Object> properties, List<String> unresolvedRefs) {
}

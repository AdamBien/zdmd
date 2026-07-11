package airhacks.zdmd.tokens.control;

import airhacks.zdmd.tokens.entity.ResolvedDimension;
import airhacks.zdmd.tokens.entity.Spec;

import java.util.regex.Pattern;

/**
 * Dimension string parsing ("42px", "1.5rem") and token reference
 * detection ("{colors.primary}").
 */
public interface Dimensions {

    Pattern DIMENSION = Pattern.compile("^(-?\\d*\\.?\\d+)([a-zA-Z%]+)$");
    Pattern TOKEN_REFERENCE = Pattern.compile("^\\{[a-zA-Z0-9._-]+\\}$");

    /**
     * Length cap keeps validation linear and prevents pathological regex
     * backtracking on oversized, attacker-supplied values.
     */
    int MAX_DIMENSION_LENGTH = 64;

    static ResolvedDimension parse(String raw) {
        if (raw == null || raw.length() > MAX_DIMENSION_LENGTH) {
            return null;
        }
        var matcher = DIMENSION.matcher(raw);
        if (!matcher.matches()) {
            return null;
        }
        return new ResolvedDimension(Double.parseDouble(matcher.group(1)), matcher.group(2));
    }

    /** Any known CSS length/percentage unit. */
    static boolean isParseable(Object raw) {
        if (!(raw instanceof String text)) {
            return false;
        }
        var parsed = parse(text);
        return parsed != null && Spec.CSS_UNITS.contains(parsed.unit());
    }

    static boolean isTokenReference(Object raw) {
        return raw instanceof String text && TOKEN_REFERENCE.matcher(text).matches();
    }

    static String referencePath(String reference) {
        return reference.substring(1, reference.length() - 1);
    }
}

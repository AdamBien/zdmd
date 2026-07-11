package airhacks.zdmd.tokens.entity;

/**
 * A lint or model diagnostic. {@code path} is the dotted token path
 * (e.g. "colors.primary") or null for document-level findings.
 */
public record Finding(Severity severity, String path, String message) {

    public static Finding error(String path, String message) {
        return new Finding(Severity.ERROR, path, message);
    }

    public static Finding warning(String path, String message) {
        return new Finding(Severity.WARNING, path, message);
    }

    public static Finding info(String path, String message) {
        return new Finding(Severity.INFO, path, message);
    }
}

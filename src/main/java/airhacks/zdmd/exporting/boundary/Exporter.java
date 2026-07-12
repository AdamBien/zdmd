package airhacks.zdmd.exporting.boundary;

import airhacks.zdmd.exporting.control.CssVars;
import airhacks.zdmd.exporting.control.Dtcg;
import airhacks.zdmd.reporting.boundary.Json;
import airhacks.zdmd.tokens.entity.DesignSystem;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Exports a resolved design system to web-standard formats:
 * CSS custom properties and W3C Design Tokens (DTCG) JSON.
 */
public interface Exporter {

    List<String> FORMATS = List.of("css-vars", "dtcg");

    static boolean supports(String format) {
        return FORMATS.contains(format);
    }

    static String export(DesignSystem state, String format, String prefix) {
        return switch (format) {
            case "css-vars" -> CssVars.serialize(CssVars.declarations(state), prefix);
            case "dtcg" -> Json.pretty(Dtcg.tokenFile(state));
            default -> throw new IllegalArgumentException("Unsupported format: " + format);
        };
    }

    static Path writeTokens(String serialized, String format, Path directory) {
        var fileName = switch (format) {
            case "css-vars" -> "tokens.css";
            case "dtcg" -> "tokens.json";
            default -> throw new IllegalArgumentException("Unsupported format: " + format);
        };
        var file = directory.resolve(fileName);
        try {
            return Files.writeString(file, serialized);
        } catch (IOException error) {
            throw new UncheckedIOException(
                    "\"%s\" could not be written: %s".formatted(file, error.getMessage()), error);
        }
    }
}

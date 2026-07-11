package airhacks.zdmd.reporting.boundary;

import java.util.List;
import java.util.Map;

/**
 * Minimal JSON writer with JSON.stringify-compatible output: two-space
 * pretty printing, integral doubles without a decimal point, insertion
 * order preserved (callers pass LinkedHashMap). Supports Map, List,
 * String, Number, Boolean, and null.
 */
public interface Json {

    static String pretty(Object value) {
        var out = new StringBuilder();
        write(out, value, 0, true);
        return out.toString();
    }

    static String compact(Object value) {
        var out = new StringBuilder();
        write(out, value, 0, false);
        return out.toString();
    }

    private static void write(StringBuilder out, Object value, int indent, boolean pretty) {
        switch (value) {
            case null -> out.append("null");
            case String text -> writeString(out, text);
            case Boolean bool -> out.append(bool);
            case Number number -> out.append(number(number));
            case Map<?, ?> map -> writeMap(out, map, indent, pretty);
            case List<?> list -> writeList(out, list, indent, pretty);
            default -> writeString(out, String.valueOf(value));
        }
    }

    private static void writeMap(StringBuilder out, Map<?, ?> map, int indent, boolean pretty) {
        if (map.isEmpty()) {
            out.append("{}");
            return;
        }
        out.append('{');
        var first = true;
        for (var entry : map.entrySet()) {
            if (!first) {
                out.append(',');
            }
            first = false;
            newline(out, indent + 1, pretty);
            writeString(out, String.valueOf(entry.getKey()));
            out.append(pretty ? ": " : ":");
            write(out, entry.getValue(), indent + 1, pretty);
        }
        newline(out, indent, pretty);
        out.append('}');
    }

    private static void writeList(StringBuilder out, List<?> list, int indent, boolean pretty) {
        if (list.isEmpty()) {
            out.append("[]");
            return;
        }
        out.append('[');
        var first = true;
        for (var item : list) {
            if (!first) {
                out.append(',');
            }
            first = false;
            newline(out, indent + 1, pretty);
            write(out, item, indent + 1, pretty);
        }
        newline(out, indent, pretty);
        out.append(']');
    }

    private static void newline(StringBuilder out, int indent, boolean pretty) {
        if (pretty) {
            out.append('\n').append("  ".repeat(indent));
        }
    }

    private static void writeString(StringBuilder out, String text) {
        out.append('"');
        for (var i = 0; i < text.length(); i++) {
            var c = text.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                default -> {
                    if (c < 0x20) {
                        out.append("\\u%04x".formatted((int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        out.append('"');
    }

    /** Integral doubles print without a decimal point, like JavaScript numbers. */
    static String number(Number value) {
        var d = value.doubleValue();
        if (d == Math.rint(d) && !Double.isInfinite(d) && Math.abs(d) < 1e15) {
            return String.valueOf((long) d);
        }
        return String.valueOf(d);
    }
}

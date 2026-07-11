package airhacks.zdmd.parsing.control;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Zero-dependency YAML subset parser covering what DESIGN.md token files use:
 * block mappings and sequences nested by indentation, quoted and plain
 * scalars, flow collections on a single line, literal/folded block scalars,
 * and comments. Anchors, aliases, tags, and multi-document streams are
 * outside the subset and raise a {@link YamlException}.
 */
public final class Yaml {

    public static final class YamlException extends RuntimeException {
        YamlException(String message) {
            super(message);
        }
    }

    private static final Pattern INTEGER = Pattern.compile("[+-]?\\d+");
    private static final Pattern FLOAT = Pattern.compile("[+-]?(\\d+\\.\\d*|\\.\\d+|\\d+)([eE][+-]?\\d+)?");
    private static final Pattern BLOCK_SCALAR = Pattern.compile("[|>][+-]?\\d?");

    private record Line(int indent, String content, int number) {
    }

    private final List<Line> lines;
    private int position;

    private Yaml(List<Line> lines) {
        this.lines = lines;
    }

    /** Parse a YAML document into maps, lists, and scalars. Returns null for empty input. */
    public static Object parse(String text) {
        var materialized = materialize(text);
        if (materialized.isEmpty()) {
            return null;
        }
        var parser = new Yaml(materialized);
        var value = parser.parseBlock(materialized.getFirst().indent());
        if (parser.position < materialized.size()) {
            throw new YamlException("Unexpected content at line " + materialized.get(parser.position).number());
        }
        return value;
    }

    private static List<Line> materialize(String text) {
        var result = new ArrayList<Line>();
        var raw = text.split("\n", -1);
        for (var i = 0; i < raw.length; i++) {
            var line = raw[i];
            var indent = 0;
            while (indent < line.length() && line.charAt(indent) == ' ') {
                indent++;
            }
            var content = line.substring(indent);
            if (content.isBlank() || content.startsWith("#")) {
                continue;
            }
            if (content.startsWith("\t")) {
                throw new YamlException("Tab used for indentation at line " + (i + 1));
            }
            if (content.strip().equals("---") || content.strip().equals("...")) {
                if (result.isEmpty()) {
                    continue;
                }
                throw new YamlException("Multi-document YAML is not supported (line " + (i + 1) + ")");
            }
            if (content.startsWith("&") || content.startsWith("*") || content.startsWith("!!")) {
                throw new YamlException("Anchors, aliases, and tags are not supported (line " + (i + 1) + ")");
            }
            result.add(new Line(indent, content.stripTrailing(), i + 1));
        }
        return result;
    }

    private Object parseBlock(int indent) {
        var first = this.lines.get(this.position);
        if (isSequenceItem(first.content())) {
            return parseSequence(indent);
        }
        return parseMapping(indent);
    }

    private static boolean isSequenceItem(String content) {
        return content.equals("-") || content.startsWith("- ");
    }

    private Object parseSequence(int indent) {
        var sequence = new ArrayList<Object>();
        while (this.position < this.lines.size()) {
            var line = this.lines.get(this.position);
            if (line.indent() != indent || !isSequenceItem(line.content())) {
                break;
            }
            var rest = line.content().equals("-") ? "" : line.content().substring(2).strip();
            if (rest.isEmpty()) {
                this.position++;
                sequence.add(nestedValueOrNull(indent));
            } else if (looksLikeMappingEntry(rest)) {
                // compact "- key: value": re-enter as a mapping at the item's virtual indent
                this.lines.set(this.position, new Line(indent + 2, rest, line.number()));
                sequence.add(parseMapping(indent + 2));
            } else {
                this.position++;
                sequence.add(scalar(rest, line.number()));
            }
        }
        return sequence;
    }

    /** Whether a sequence item body is a compact mapping ("- key: value") rather than a scalar. */
    private static boolean looksLikeMappingEntry(String rest) {
        if (rest.startsWith("[") || rest.startsWith("{")) {
            return false;
        }
        if (rest.startsWith("\"") || rest.startsWith("'")) {
            var end = closingQuote(rest, rest.charAt(0));
            return end >= 0 && rest.substring(end + 1).stripLeading().startsWith(":");
        }
        return mappingColon(rest) >= 0;
    }

    private Object parseMapping(int indent) {
        var mapping = new LinkedHashMap<String, Object>();
        while (this.position < this.lines.size()) {
            var line = this.lines.get(this.position);
            if (line.indent() != indent || isSequenceItem(line.content())) {
                break;
            }
            var entry = splitKey(line.content(), line.number());
            if (mapping.containsKey(entry.key())) {
                throw new YamlException("Map keys must be unique: '" + entry.key() + "' at line " + line.number());
            }
            this.position++;
            mapping.put(entry.key(), entryValue(entry.rawValue(), indent, line.number()));
        }
        if (mapping.isEmpty()) {
            var line = this.lines.get(this.position);
            throw new YamlException("Expected a mapping entry at line " + line.number());
        }
        return mapping;
    }

    private Object entryValue(String rawValue, int indent, int lineNumber) {
        if (rawValue.isEmpty()) {
            return nestedValueOrNull(indent);
        }
        if (BLOCK_SCALAR.matcher(rawValue).matches()) {
            return blockScalar(rawValue.charAt(0), indent);
        }
        return scalar(rawValue, lineNumber);
    }

    /** Value on the following lines: nested block if more indented, sequence allowed at same indent. */
    private Object nestedValueOrNull(int indent) {
        if (this.position >= this.lines.size()) {
            return null;
        }
        var next = this.lines.get(this.position);
        if (next.indent() > indent) {
            return parseBlock(next.indent());
        }
        if (next.indent() == indent && isSequenceItem(next.content())) {
            return parseSequence(indent);
        }
        return null;
    }

    private String blockScalar(char style, int parentIndent) {
        var collected = new ArrayList<String>();
        Integer blockIndent = null;
        while (this.position < this.lines.size()) {
            var line = this.lines.get(this.position);
            if (line.indent() <= parentIndent) {
                break;
            }
            blockIndent = blockIndent == null ? line.indent() : blockIndent;
            var padding = " ".repeat(Math.max(0, line.indent() - blockIndent));
            collected.add(padding + line.content());
            this.position++;
        }
        var joined = style == '|'
                ? String.join("\n", collected)
                : String.join(" ", collected);
        return joined + "\n";
    }

    private record Entry(String key, String rawValue) {
    }

    private Entry splitKey(String content, int lineNumber) {
        if (content.startsWith("\"") || content.startsWith("'")) {
            var quote = content.charAt(0);
            var end = closingQuote(content, quote);
            if (end < 0) {
                throw new YamlException("Unterminated quoted key at line " + lineNumber);
            }
            var key = unquote(content.substring(0, end + 1));
            var rest = content.substring(end + 1).stripLeading();
            if (!rest.startsWith(":")) {
                throw new YamlException("Expected ':' after key at line " + lineNumber);
            }
            return new Entry(key, stripComment(rest.substring(1)).strip());
        }
        var colon = mappingColon(content);
        if (colon < 0) {
            throw new YamlException("Expected 'key: value' at line " + lineNumber);
        }
        return new Entry(content.substring(0, colon).strip(), stripComment(content.substring(colon + 1)).strip());
    }

    /** First ':' that is followed by a space or ends the line — the mapping separator. */
    private static int mappingColon(String content) {
        for (var i = 0; i < content.length(); i++) {
            if (content.charAt(i) == ':' && (i + 1 == content.length() || content.charAt(i + 1) == ' ')) {
                return i;
            }
        }
        return -1;
    }

    private static int closingQuote(String content, char quote) {
        for (var i = 1; i < content.length(); i++) {
            var c = content.charAt(i);
            if (quote == '"' && c == '\\') {
                i++;
            } else if (c == quote) {
                if (quote == '\'' && i + 1 < content.length() && content.charAt(i + 1) == '\'') {
                    i++;
                } else {
                    return i;
                }
            }
        }
        return -1;
    }

    private static String stripComment(String value) {
        var inSingle = false;
        var inDouble = false;
        for (var i = 0; i < value.length(); i++) {
            var c = value.charAt(i);
            if (c == '\\' && inDouble) {
                i++;
            } else if (c == '\'' && !inDouble) {
                inSingle = !inSingle;
            } else if (c == '"' && !inSingle) {
                inDouble = !inDouble;
            } else if (c == '#' && !inSingle && !inDouble && i > 0 && value.charAt(i - 1) == ' ') {
                return value.substring(0, i);
            }
        }
        return value;
    }

    private Object scalar(String raw, int lineNumber) {
        var value = raw.strip();
        if (value.startsWith("\"") || value.startsWith("'")) {
            var end = closingQuote(value, value.charAt(0));
            if (end < 0) {
                throw new YamlException("Unterminated quoted scalar at line " + lineNumber);
            }
            return unquote(value.substring(0, end + 1));
        }
        if (value.startsWith("[") || value.startsWith("{")) {
            return flow(value, lineNumber);
        }
        if (value.startsWith("&") || value.startsWith("*") || value.startsWith("!!")) {
            throw new YamlException("Anchors, aliases, and tags are not supported (line " + lineNumber + ")");
        }
        return switch (value) {
            case "true", "True", "TRUE" -> Boolean.TRUE;
            case "false", "False", "FALSE" -> Boolean.FALSE;
            case "null", "Null", "NULL", "~", "" -> null;
            default -> typed(value);
        };
    }

    private static Object typed(String value) {
        if (INTEGER.matcher(value).matches()) {
            try {
                return Integer.valueOf(value);
            } catch (NumberFormatException tooLarge) {
                return Long.valueOf(value);
            }
        }
        if (FLOAT.matcher(value).matches() && (value.contains(".") || value.contains("e") || value.contains("E"))) {
            return Double.valueOf(value);
        }
        return value;
    }

    private static String unquote(String quoted) {
        var inner = quoted.substring(1, quoted.length() - 1);
        if (quoted.startsWith("'")) {
            return inner.replace("''", "'");
        }
        var result = new StringBuilder();
        for (var i = 0; i < inner.length(); i++) {
            var c = inner.charAt(i);
            if (c == '\\' && i + 1 < inner.length()) {
                i++;
                result.append(switch (inner.charAt(i)) {
                    case 'n' -> '\n';
                    case 't' -> '\t';
                    case 'r' -> '\r';
                    case '"' -> '"';
                    case '\\' -> '\\';
                    case '0' -> '\0';
                    default -> inner.charAt(i);
                });
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    private Object flow(String value, int lineNumber) {
        var closing = value.startsWith("[") ? ']' : '}';
        if (value.length() < 2 || value.charAt(value.length() - 1) != closing) {
            throw new YamlException("Unterminated flow collection at line " + lineNumber);
        }
        var inner = value.substring(1, value.length() - 1).strip();
        if (value.startsWith("[")) {
            var list = new ArrayList<Object>();
            for (var item : splitFlow(inner)) {
                list.add(scalar(item, lineNumber));
            }
            return list;
        }
        var map = new LinkedHashMap<String, Object>();
        for (var item : splitFlow(inner)) {
            var entry = splitKey(item, lineNumber);
            map.put(entry.key(), scalar(entry.rawValue(), lineNumber));
        }
        return map;
    }

    private static List<String> splitFlow(String inner) {
        var items = new ArrayList<String>();
        if (inner.isEmpty()) {
            return items;
        }
        var depth = 0;
        var inSingle = false;
        var inDouble = false;
        var start = 0;
        for (var i = 0; i < inner.length(); i++) {
            var c = inner.charAt(i);
            if (c == '\\' && inDouble) {
                i++;
            } else if (c == '\'' && !inDouble) {
                inSingle = !inSingle;
            } else if (c == '"' && !inSingle) {
                inDouble = !inDouble;
            } else if (!inSingle && !inDouble) {
                if (c == '[' || c == '{') {
                    depth++;
                } else if (c == ']' || c == '}') {
                    depth--;
                } else if (c == ',' && depth == 0) {
                    items.add(inner.substring(start, i).strip());
                    start = i + 1;
                }
            }
        }
        items.add(inner.substring(start).strip());
        return items;
    }
}

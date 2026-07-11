package airhacks.zdmd.parsing.control;

import airhacks.zdmd.parsing.entity.DocumentSection;
import airhacks.zdmd.parsing.entity.ScannedDocument;
import airhacks.zdmd.parsing.entity.ScannedDocument.Heading;
import airhacks.zdmd.parsing.entity.YamlBlock;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Line-based markdown scanner covering the subset DESIGN.md needs:
 * YAML frontmatter, fenced yaml/yml code blocks, and ATX H2 headings.
 * Fence state is tracked so headings inside code blocks are ignored.
 * Setext headings and fences indented four or more spaces are out of scope.
 */
public interface MarkdownScanner {

    Pattern FENCE_OPEN = Pattern.compile("^ {0,3}(`{3,}|~{3,})(.*)$");
    Pattern H2 = Pattern.compile("^ {0,3}##(?:\\s+(.*))?$");
    Pattern ATX_CLOSING = Pattern.compile("\\s#+\\s*$");

    static ScannedDocument scan(String content) {
        var lines = content.split("\n", -1);
        var blocks = new ArrayList<YamlBlock>();
        var headings = new ArrayList<Heading>();

        var index = 0;
        var current = 0;

        if (lines.length > 0 && lines[0].strip().equals("---") && !lines[0].startsWith(" ")) {
            var close = closingFrontmatterLine(lines);
            if (close > 0) {
                var yaml = String.join("\n", List.of(lines).subList(1, close));
                blocks.add(new YamlBlock(yaml, true, -1, 1));
                current = close + 1;
            }
        }

        String fenceMarker = null;
        var fenceIsYaml = false;
        var fenceStartLine = 0;
        var fenceContent = new ArrayList<String>();

        for (var i = current; i < lines.length; i++) {
            var line = lines[i];
            if (fenceMarker != null) {
                if (closesFence(line, fenceMarker)) {
                    if (fenceIsYaml) {
                        blocks.add(new YamlBlock(String.join("\n", fenceContent), false, index, fenceStartLine));
                        index++;
                    }
                    fenceMarker = null;
                    fenceContent.clear();
                } else {
                    fenceContent.add(line);
                }
                continue;
            }

            var fence = FENCE_OPEN.matcher(line);
            if (fence.matches()) {
                var marker = fence.group(1);
                var info = fence.group(2).strip();
                if (marker.startsWith("`") && info.contains("`")) {
                    continue;
                }
                fenceMarker = marker;
                var lang = info.isEmpty() ? "" : info.split("\\s+")[0];
                fenceIsYaml = lang.equals("yaml") || lang.equals("yml");
                fenceStartLine = i + 1;
                continue;
            }

            var heading = H2.matcher(line);
            if (heading.matches()) {
                var text = headingText(heading.group(1));
                if (!text.isEmpty()) {
                    headings.add(new Heading(text, i + 1));
                }
            }
        }

        // unclosed yaml fence: remark still yields a code node to end of input
        if (fenceMarker != null && fenceIsYaml) {
            blocks.add(new YamlBlock(String.join("\n", fenceContent), false, index, fenceStartLine));
        }

        return new ScannedDocument(blocks, headings, sliceSections(lines, headings, content));
    }

    private static int closingFrontmatterLine(String[] lines) {
        for (var i = 1; i < lines.length; i++) {
            if (lines[i].strip().equals("---")) {
                return i;
            }
        }
        return -1;
    }

    private static boolean closesFence(String line, String marker) {
        var stripped = line.strip();
        if (stripped.length() < marker.length()) {
            return false;
        }
        var markerChar = marker.charAt(0);
        return stripped.chars().allMatch(c -> c == markerChar) && line.indexOf(markerChar) <= 3;
    }

    private static String headingText(String rawText) {
        if (rawText == null) {
            return "";
        }
        return ATX_CLOSING.matcher(rawText.strip()).replaceAll("").strip();
    }

    /**
     * Partition the document into sections along H2 heading lines,
     * with an optional prelude before the first heading.
     */
    private static List<DocumentSection> sliceSections(String[] lines, List<Heading> headings, String content) {
        var sections = new ArrayList<DocumentSection>();
        if (headings.isEmpty()) {
            sections.add(new DocumentSection("", content));
            return sections;
        }
        var all = List.of(lines);
        var firstLine = headings.getFirst().line();
        if (firstLine > 1) {
            sections.add(new DocumentSection("", String.join("\n", all.subList(0, firstLine - 1))));
        }
        for (var i = 0; i < headings.size(); i++) {
            var heading = headings.get(i);
            var start = heading.line() - 1;
            var end = i + 1 < headings.size() ? headings.get(i + 1).line() - 1 : lines.length;
            sections.add(new DocumentSection(heading.text(), String.join("\n", all.subList(start, end))));
        }
        return sections;
    }
}

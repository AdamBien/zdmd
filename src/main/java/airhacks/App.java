package airhacks;

import airhacks.zdmd.diffing.boundary.Differ;
import airhacks.zdmd.exporting.boundary.Exporter;
import airhacks.zdmd.linting.boundary.Linter;
import airhacks.zdmd.linting.entity.LintReport;
import airhacks.zdmd.reporting.boundary.Json;
import airhacks.zdmd.reporting.boundary.Markdown;
import airhacks.zdmd.tokens.entity.Finding;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * zdmd — agent-first CLI for DESIGN.md: lint, diff, and export design
 * tokens to web-standard formats (CSS custom properties, W3C DTCG JSON).
 */
public interface App {

    String VERSION = Version.get();

    static void main(String... args) {
        System.exit(run(args));
    }

    static int run(String... args) {
        if (args.length == 0 || args[0].equals("help") || args[0].equals("--help") || args[0].equals("-h")) {
            IO.println(usage());
            return 0;
        }
        if (args[0].equals("--version") || args[0].equals("-v")) {
            IO.println(VERSION);
            return 0;
        }
        var arguments = Arguments.parse(args);
        try {
            return switch (args[0]) {
                case "lint" -> lint(arguments);
                case "diff" -> diff(arguments);
                case "export" -> export(arguments);
                default -> {
                    System.err.println("Unknown command: " + args[0]);
                    System.err.println(usage());
                    yield 1;
                }
            };
        } catch (FileReadException error) {
            System.err.println("Error: " + error.getMessage());
            return 2;
        } catch (RuntimeException error) {
            System.err.println(error.getMessage());
            return 1;
        }
    }

    static String usage() {
        return """
                zdmd %s — lint, diff, and export DESIGN.md design tokens

                Usage:
                  zdmd lint <file> [--format json|md]
                  zdmd diff <before> <after> [--format json|md]
                  zdmd export <file> --format <css-vars|dtcg> [--prefix <prefix>]

                Pass "-" as a file to read from stdin.
                """.formatted(VERSION).strip();
    }

    static int lint(Arguments arguments) {
        var file = arguments.positional(0, "lint requires a file argument");
        var report = Linter.lint(readInput(file));

        var output = new LinkedHashMap<String, Object>();
        output.put("findings", findingMaps(report.findings()));
        output.put("summary", summaryMap(report));
        IO.println(format(output, arguments.flag("format")));
        return report.summary().errors() > 0 ? 1 : 0;
    }

    static int diff(Arguments arguments) {
        var before = arguments.positional(0, "diff requires <before> and <after> file arguments");
        var after = arguments.positional(1, "diff requires <before> and <after> file arguments");
        var beforeReport = Linter.lint(readInput(before));
        var afterReport = Linter.lint(readInput(after));

        var result = Differ.diff(beforeReport, afterReport);
        IO.println(format(result, arguments.flag("format")));
        return Differ.regression(result) ? 1 : 0;
    }

    static int export(Arguments arguments) {
        var format = arguments.flag("format");
        if (format == null || !Exporter.supports(format)) {
            var error = new LinkedHashMap<String, Object>();
            error.put("error", "INVALID_FORMAT");
            error.put("message", "Invalid format \"%s\". Valid formats: %s"
                    .formatted(format, String.join(", ", Exporter.FORMATS)));
            System.err.println(Json.compact(error));
            return 1;
        }
        var file = arguments.positional(0, "export requires a file argument");
        var report = Linter.lint(readInput(file));
        IO.println(Exporter.export(report.designSystem(), format, arguments.flag("prefix")));
        // a successful export exits 0 even if the source has lint findings;
        // those are surfaced by `lint`, not by the export itself
        return 0;
    }

    private static String format(Map<String, Object> output, String format) {
        if ("markdown".equals(format) || "md".equals(format)) {
            return Markdown.format(output);
        }
        return Json.pretty(output);
    }

    private static List<Map<String, Object>> findingMaps(List<Finding> findings) {
        var maps = new ArrayList<Map<String, Object>>();
        for (var finding : findings) {
            var map = new LinkedHashMap<String, Object>();
            map.put("severity", finding.severity().label);
            if (finding.path() != null) {
                map.put("path", finding.path());
            }
            map.put("message", finding.message());
            maps.add(map);
        }
        return maps;
    }

    private static Map<String, Object> summaryMap(LintReport report) {
        var map = new LinkedHashMap<String, Object>();
        map.put("errors", report.summary().errors());
        map.put("warnings", report.summary().warnings());
        map.put("infos", report.summary().infos());
        return map;
    }

    class FileReadException extends RuntimeException {
        FileReadException(String message) {
            super(message);
        }
    }

    /** Read a file, or stdin when the path is "-". */
    private static String readInput(String filePath) {
        if (filePath.equals("-")) {
            var console = System.console();
            if (console != null && console.isTerminal()) {
                System.err.println("Reading from stdin… Press Ctrl+D when done.");
            }
            try {
                return new String(System.in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            } catch (IOException error) {
                throw new FileReadException("stdin could not be read: " + error.getMessage());
            }
        }
        try {
            return Files.readString(Path.of(filePath));
        } catch (NoSuchFileException notFound) {
            throw new FileReadException(
                    "\"%s\" not found. Create a DESIGN.md file or pass \"-\" to read from stdin.".formatted(filePath));
        } catch (AccessDeniedException denied) {
            throw new FileReadException("\"%s\" could not be read: permission denied.".formatted(filePath));
        } catch (IOException error) {
            throw new FileReadException("\"%s\" could not be read: %s".formatted(filePath, error.getMessage()));
        }
    }

    /** Positional arguments and --flag values, with --flag=value support. */
    record Arguments(List<String> positionals, Map<String, String> flags) {

        static Arguments parse(String[] args) {
            var positionals = new ArrayList<String>();
            var flags = new LinkedHashMap<String, String>();
            for (var i = 1; i < args.length; i++) {
                var arg = args[i];
                if (arg.startsWith("--")) {
                    var name = arg.substring(2);
                    var equals = name.indexOf('=');
                    if (equals >= 0) {
                        flags.put(name.substring(0, equals), name.substring(equals + 1));
                    } else if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                        flags.put(name, args[++i]);
                    } else {
                        flags.put(name, "");
                    }
                } else {
                    positionals.add(arg);
                }
            }
            return new Arguments(positionals, flags);
        }

        String positional(int index, String missingMessage) {
            if (index >= this.positionals.size()) {
                throw new IllegalArgumentException(missingMessage);
            }
            return this.positionals.get(index);
        }

        String flag(String name) {
            return this.flags.get(name);
        }
    }
}

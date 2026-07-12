import airhacks.App;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

void main() throws Exception {
    var clean = Files.createTempFile("design-clean", ".md");
    Files.writeString(clean, """
            ```yaml
            colors:
              primary: "#111111"
            ```
            """);
    var broken = Files.createTempFile("design-broken", ".md");
    Files.writeString(broken, """
            ```yaml
            colors:
              broken: nope
            ```
            """);
    try {
        // covers S1.1
        var success = App.run("lint", clean.toString());
        assert success == 0 : "clean lint exits 0, got " + success;

        // covers S1.3
        var lintErrors = App.run("lint", broken.toString());
        assert lintErrors == 1 : "lint with errors exits 1, got " + lintErrors;
        var regression = App.run("diff", clean.toString(), broken.toString());
        assert regression == 1 : "diff regression exits 1, got " + regression;

        // covers S1.2
        var unreadable = App.run("lint", "/nope/missing-design.md");
        assert unreadable == 2 : "unreadable input exits 2, got " + unreadable;

        // covers S1.4
        var export = App.run("export", broken.toString(), "--format", "css-vars");
        assert export == 0 : "successful export exits 0 despite lint findings, got " + export;

        // covers S1.5
        assert Files.exists(Path.of("tokens.css")) : "successful export also writes tokens.css to the working directory";

        // covers S2.2
        assert !Files.exists(Path.of("DESIGN.md")) : "precondition: working directory must not contain DESIGN.md";
        var missing = App.run();
        assert missing == 2 : "bare launch without DESIGN.md exits 2, got " + missing;

        // covers S2.1, S2.3
        Files.copy(broken, Path.of("DESIGN.md"));
        var stdout = new ByteArrayOutputStream();
        var original = System.out;
        System.setOut(new PrintStream(stdout));
        int bare;
        try {
            bare = App.run();
        } finally {
            System.setOut(original);
        }
        assert bare == 0 : "bare launch exits 0 regardless of lint findings, got " + bare;
        assert Files.exists(Path.of("tokens.css")) && Files.exists(Path.of("tokens.json"))
                : "bare launch writes every supported format";
        var printed = stdout.toString();
        assert printed.contains("tokens.css") && printed.contains("tokens.json")
                : "bare launch prints each written filename, got: " + printed;
    } finally {
        Files.deleteIfExists(clean);
        Files.deleteIfExists(broken);
        Files.deleteIfExists(Path.of("DESIGN.md"));
        Files.deleteIfExists(Path.of("tokens.css"));
        Files.deleteIfExists(Path.of("tokens.json"));
    }
}

import airhacks.App;

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
    } finally {
        Files.deleteIfExists(clean);
        Files.deleteIfExists(broken);
        Files.deleteIfExists(Path.of("tokens.css"));
        Files.deleteIfExists(Path.of("tokens.json"));
    }
}

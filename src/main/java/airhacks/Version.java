package airhacks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public interface Version {

    static String get() {
        var fromManifest = Version.class.getPackage().getImplementationVersion();
        if (fromManifest != null)
            return fromManifest.strip();
        try (var in = Version.class.getClassLoader().getResourceAsStream("version.txt")) {
            if (in != null)
                return new String(in.readAllBytes()).strip();
        } catch (IOException fromClasspathFailed) {
            // fall through to the filesystem
        }
        try {
            return Files.readString(Path.of("version.txt")).strip();
        } catch (IOException fromFilesystemFailed) {
            return "unknown";
        }
    }
}

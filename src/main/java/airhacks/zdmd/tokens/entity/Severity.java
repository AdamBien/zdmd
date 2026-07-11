package airhacks.zdmd.tokens.entity;

public enum Severity {
    ERROR("error"),
    WARNING("warning"),
    INFO("info");

    public final String label;

    Severity(String label) {
        this.label = label;
    }
}

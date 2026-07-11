import airhacks.zdmd.parsing.control.Yaml;

import java.util.List;
import java.util.Map;

void main() {
    nestedMappings();
    scalarTypes();
    quotedStrings();
    comments();
    sequences();
    flowCollections();
    blockScalar();
    errors();
}

// covers parsing R3.1
void nestedMappings() {
    var parsed = (Map<?, ?>) Yaml.parse("""
            colors:
              primary: "#1A1C1E"
              nested:
                light: "#ffffff"
            """);
    var colors = (Map<?, ?>) parsed.get("colors");
    assert "#1A1C1E".equals(colors.get("primary")) : "expected #1A1C1E but got " + colors.get("primary");
    var nested = (Map<?, ?>) colors.get("nested");
    assert "#ffffff".equals(nested.get("light")) : "expected #ffffff but got " + nested.get("light");
}

// covers parsing R3.1
void scalarTypes() {
    var parsed = (Map<?, ?>) Yaml.parse("""
            weight: 600
            ratio: 1.5
            flag: true
            missing: null
            tilde: ~
            text: Public Sans
            """);
    assert Integer.valueOf(600).equals(parsed.get("weight")) : "expected 600 but got " + parsed.get("weight");
    assert Double.valueOf(1.5).equals(parsed.get("ratio")) : "expected 1.5 but got " + parsed.get("ratio");
    assert Boolean.TRUE.equals(parsed.get("flag")) : "expected true but got " + parsed.get("flag");
    assert parsed.get("missing") == null : "expected null but got " + parsed.get("missing");
    assert parsed.get("tilde") == null : "expected null but got " + parsed.get("tilde");
    assert "Public Sans".equals(parsed.get("text")) : "expected 'Public Sans' but got " + parsed.get("text");
}

// covers parsing R3.1
void quotedStrings() {
    var parsed = (Map<?, ?>) Yaml.parse("""
            double: "a \\"quoted\\" value"
            single: 'it''s fine'
            spacing: "-0.02em"
            "quoted key": ok
            """);
    assert "a \"quoted\" value".equals(parsed.get("double")) : "double quotes: got " + parsed.get("double");
    assert "it's fine".equals(parsed.get("single")) : "single quotes: got " + parsed.get("single");
    assert "-0.02em".equals(parsed.get("spacing")) : "expected -0.02em but got " + parsed.get("spacing");
    assert "ok".equals(parsed.get("quoted key")) : "quoted key: got " + parsed.get("quoted key");
}

// covers parsing R3.1
void comments() {
    var parsed = (Map<?, ?>) Yaml.parse("""
            # full line comment
            color: "#fff" # inline comment
            url: http://example.com#anchor
            """);
    assert "#fff".equals(parsed.get("color")) : "expected #fff but got " + parsed.get("color");
    assert "http://example.com#anchor".equals(parsed.get("url")) : "hash without space kept: got " + parsed.get("url");
}

// covers parsing R3.1
void sequences() {
    var parsed = (Map<?, ?>) Yaml.parse("""
            units:
              - px
              - rem
            compact:
              - name: fontFamily
                type: string
            """);
    assert List.of("px", "rem").equals(parsed.get("units")) : "expected [px, rem] but got " + parsed.get("units");
    var compact = (List<?>) parsed.get("compact");
    var first = (Map<?, ?>) compact.getFirst();
    assert "fontFamily".equals(first.get("name")) : "compact mapping: got " + first.get("name");
    assert "string".equals(first.get("type")) : "compact mapping second key: got " + first.get("type");
}

// covers parsing R3.1
void flowCollections() {
    var parsed = (Map<?, ?>) Yaml.parse("""
            list: [1, 2, 3]
            map: {a: 1, b: two}
            """);
    assert List.of(1, 2, 3).equals(parsed.get("list")) : "flow list: got " + parsed.get("list");
    var map = (Map<?, ?>) parsed.get("map");
    assert Integer.valueOf(1).equals(map.get("a")) : "flow map: got " + map.get("a");
    assert "two".equals(map.get("b")) : "flow map: got " + map.get("b");
}

// covers parsing R3.1
void blockScalar() {
    var parsed = (Map<?, ?>) Yaml.parse("""
            literal: |
              line one
              line two
            """);
    assert "line one\nline two\n".equals(parsed.get("literal")) : "literal block: got " + parsed.get("literal");
}

// covers parsing R3.2
void errors() {
    try {
        Yaml.parse("key: 1\nkey: 2\n");
        throw new AssertionError("duplicate keys must be rejected");
    } catch (Yaml.YamlException expected) {
    }
    try {
        Yaml.parse("key:\n\tnested: 1\n");
        throw new AssertionError("tab indentation must be rejected");
    } catch (Yaml.YamlException expected) {
    }
    try {
        Yaml.parse("anchor: &a value\n");
        throw new AssertionError("anchors must be rejected");
    } catch (Yaml.YamlException expected) {
    }
}

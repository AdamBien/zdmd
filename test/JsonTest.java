import airhacks.zdmd.reporting.boundary.Json;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

void main() {
    numbers();
    prettyFormat();
    escaping();
}

// covers reporting R1.1
void numbers() {
    assert "16".equals(Json.number(16.0)) : "integral double without decimal: got " + Json.number(16.0);
    assert "0.75".equals(Json.number(0.75)) : "fraction kept: got " + Json.number(0.75);
    assert "-2".equals(Json.number(-2.0)) : "negative integral: got " + Json.number(-2.0);
    assert "0.102".equals(Json.number(0.102)) : "three decimals: got " + Json.number(0.102);
}

// covers reporting R1.1, reporting R1.3
void prettyFormat() {
    var map = new LinkedHashMap<String, Object>();
    map.put("a", 1);
    map.put("b", List.of("x"));
    map.put("empty", Map.of());
    var expected = """
            {
              "a": 1,
              "b": [
                "x"
              ],
              "empty": {}
            }""";
    var actual = Json.pretty(map);
    assert expected.equals(actual) : "pretty format mismatch:\n" + actual;
    assert "{\"a\":1}".equals(Json.compact(Map.of("a", 1))) : "compact: got " + Json.compact(Map.of("a", 1));
}

// covers reporting R1.2
void escaping() {
    var escaped = Json.compact("say \"hi\"\nnew\tline\\");
    assert "\"say \\\"hi\\\"\\nnew\\tline\\\\\"".equals(escaped) : "escaping: got " + escaped;
}

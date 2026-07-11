package airhacks.zdmd.diffing.boundary;

import airhacks.zdmd.linting.entity.LintReport;
import airhacks.zdmd.linting.entity.Summary;
import airhacks.zdmd.tokens.entity.ComponentDef;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Compares two lint reports: token-level added/removed/modified keys per
 * section, finding-count deltas, and a regression flag when errors or
 * warnings increased.
 */
public interface Differ {

    static Map<String, Object> diff(LintReport before, LintReport after) {
        var tokens = new LinkedHashMap<String, Object>();
        tokens.put("colors", diffMaps(before.designSystem().colors(), after.designSystem().colors()));
        tokens.put("typography", diffMaps(before.designSystem().typography(), after.designSystem().typography()));
        tokens.put("rounded", diffMaps(before.designSystem().rounded(), after.designSystem().rounded()));
        tokens.put("spacing", diffMaps(before.designSystem().spacing(), after.designSystem().spacing()));
        tokens.put("components", diffMaps(
                properties(before.designSystem().components()),
                properties(after.designSystem().components())));

        var delta = new LinkedHashMap<String, Object>();
        delta.put("errors", after.summary().errors() - before.summary().errors());
        delta.put("warnings", after.summary().warnings() - before.summary().warnings());

        var findings = new LinkedHashMap<String, Object>();
        findings.put("before", summaryMap(before.summary()));
        findings.put("after", summaryMap(after.summary()));
        findings.put("delta", delta);

        var regression = after.summary().errors() > before.summary().errors()
                || after.summary().warnings() > before.summary().warnings();

        var result = new LinkedHashMap<String, Object>();
        result.put("tokens", tokens);
        result.put("findings", findings);
        result.put("regression", regression);
        return result;
    }

    static boolean regression(Map<String, Object> diff) {
        return Boolean.TRUE.equals(diff.get("regression"));
    }

    private static Map<String, Object> diffMaps(Map<String, ?> before, Map<String, ?> after) {
        var added = new ArrayList<String>();
        var removed = new ArrayList<String>();
        var modified = new ArrayList<String>();

        for (var key : after.keySet()) {
            if (!before.containsKey(key)) {
                added.add(key);
            } else if (!Objects.equals(before.get(key), after.get(key))) {
                modified.add(key);
            }
        }
        for (var key : before.keySet()) {
            if (!after.containsKey(key)) {
                removed.add(key);
            }
        }

        var result = new LinkedHashMap<String, Object>();
        result.put("added", added);
        result.put("removed", removed);
        result.put("modified", modified);
        return result;
    }

    private static Map<String, Map<String, Object>> properties(Map<String, ComponentDef> components) {
        var result = new LinkedHashMap<String, Map<String, Object>>();
        components.forEach((name, component) -> result.put(name, component.properties()));
        return result;
    }

    private static Map<String, Object> summaryMap(Summary summary) {
        var map = new LinkedHashMap<String, Object>();
        map.put("errors", summary.errors());
        map.put("warnings", summary.warnings());
        map.put("infos", summary.infos());
        return map;
    }
}

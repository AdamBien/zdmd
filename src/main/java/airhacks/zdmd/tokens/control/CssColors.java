package airhacks.zdmd.tokens.control;

import airhacks.zdmd.tokens.entity.ResolvedColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * CSS color parser: hex, named colors, rgb/hsl/hwb/lab/lch/oklab/oklch
 * functional notations, and color-mix in srgb. Produces sRGB components
 * plus WCAG relative luminance. Returns null for invalid colors.
 */
public interface CssColors {

    Pattern HEX = Pattern.compile("^#([0-9a-f]{3,4}|[0-9a-f]{6}|[0-9a-f]{8})$");
    Pattern FUNC = Pattern.compile("^([a-z-]{3,15})\\((.*)\\)$", Pattern.CASE_INSENSITIVE);
    Pattern FLOAT_PREFIX = Pattern.compile("^[+-]?(\\d+\\.?\\d*|\\.\\d+)([eE][+-]?\\d+)?");

    /** Guards against stack exhaustion from pathologically nested color-mix() values. */
    int MAX_COLOR_MIX_DEPTH = 32;

    Map<String, String> NAMED = Map.ofEntries(
            Map.entry("aliceblue", "#f0f8ff"), Map.entry("antiquewhite", "#faebd7"), Map.entry("aqua", "#00ffff"),
            Map.entry("aquamarine", "#7fffd4"), Map.entry("azure", "#f0ffff"), Map.entry("beige", "#f5f5dc"),
            Map.entry("bisque", "#ffe4c4"), Map.entry("black", "#000000"), Map.entry("blanchedalmond", "#ffebcd"),
            Map.entry("blue", "#0000ff"), Map.entry("blueviolet", "#8a2be2"), Map.entry("brown", "#a52a2a"),
            Map.entry("burlywood", "#deb887"), Map.entry("cadetblue", "#5f9ea0"), Map.entry("chartreuse", "#7fff00"),
            Map.entry("chocolate", "#d2691e"), Map.entry("coral", "#ff7f50"), Map.entry("cornflowerblue", "#6495ed"),
            Map.entry("cornsilk", "#fff8dc"), Map.entry("crimson", "#dc143c"), Map.entry("cyan", "#00ffff"),
            Map.entry("darkblue", "#00008b"), Map.entry("darkcyan", "#008b8b"), Map.entry("darkgoldenrod", "#b8860b"),
            Map.entry("darkgray", "#a9a9a9"), Map.entry("darkgrey", "#a9a9a9"), Map.entry("darkgreen", "#006400"),
            Map.entry("darkkhaki", "#bdb76b"), Map.entry("darkmagenta", "#8b008b"),
            Map.entry("darkolivegreen", "#556b2f"), Map.entry("darkorange", "#ff8c00"),
            Map.entry("darkorchid", "#9932cc"), Map.entry("darkred", "#8b0000"), Map.entry("darksalmon", "#e9967a"),
            Map.entry("darkseagreen", "#8fbc8f"), Map.entry("darkslateblue", "#483d8b"),
            Map.entry("darkslategrey", "#2f4f4f"), Map.entry("darkslategray", "#2f4f4f"),
            Map.entry("darkturquoise", "#00ced1"), Map.entry("darkviolet", "#9400d3"),
            Map.entry("deeppink", "#ff1493"), Map.entry("deepskyblue", "#00bfff"), Map.entry("dimgray", "#696969"),
            Map.entry("dimgrey", "#696969"), Map.entry("dodgerblue", "#1e90ff"), Map.entry("firebrick", "#b22222"),
            Map.entry("floralwhite", "#fffaf0"), Map.entry("forestgreen", "#228b22"),
            Map.entry("fuchsia", "#ff00ff"), Map.entry("gainsboro", "#dcdcdc"), Map.entry("ghostwhite", "#f8f8ff"),
            Map.entry("gold", "#ffd700"), Map.entry("goldenrod", "#daa520"), Map.entry("gray", "#808080"),
            Map.entry("grey", "#808080"), Map.entry("green", "#008000"), Map.entry("greenyellow", "#adff2f"),
            Map.entry("honeydew", "#f0fff0"), Map.entry("hotpink", "#ff69b4"), Map.entry("indianred", "#cd5c5c"),
            Map.entry("indigo", "#4b0082"), Map.entry("ivory", "#fffff0"), Map.entry("khaki", "#f0e68c"),
            Map.entry("lavender", "#e6e6fa"), Map.entry("lavenderblush", "#fff0f5"),
            Map.entry("lawngreen", "#7cfc00"), Map.entry("lemonchiffon", "#fffacd"),
            Map.entry("lightblue", "#add8e6"), Map.entry("lightcoral", "#f08080"), Map.entry("lightcyan", "#e0ffff"),
            Map.entry("lightgoldenrodyellow", "#fafad2"), Map.entry("lightgray", "#d3d3d3"),
            Map.entry("lightgrey", "#d3d3d3"), Map.entry("lightgreen", "#90ee90"), Map.entry("lightpink", "#ffb6c1"),
            Map.entry("lightsalmon", "#ffa07a"), Map.entry("lightseagreen", "#20b2aa"),
            Map.entry("lightskyblue", "#87cefa"), Map.entry("lightslate", "#778899"),
            Map.entry("lightslategray", "#778899"), Map.entry("lightslategrey", "#778899"),
            Map.entry("lightsteelblue", "#b0c4de"), Map.entry("lightyellow", "#ffffe0"),
            Map.entry("lime", "#00ff00"), Map.entry("limegreen", "#32cd32"), Map.entry("linen", "#faf0e6"),
            Map.entry("magenta", "#ff00ff"), Map.entry("maroon", "#800000"),
            Map.entry("mediumaquamarine", "#66cdaa"), Map.entry("mediumblue", "#0000cd"),
            Map.entry("mediumorchid", "#ba55d3"), Map.entry("mediumpurple", "#9370db"),
            Map.entry("mediumseagreen", "#3cb371"), Map.entry("mediumslateblue", "#7b68ee"),
            Map.entry("mediumspringgreen", "#00fa9a"), Map.entry("mediumturquoise", "#48d1cc"),
            Map.entry("mediumvioletred", "#c71585"), Map.entry("midnightblue", "#191970"),
            Map.entry("mintcream", "#f5fffa"), Map.entry("mistyrose", "#ffe4e1"), Map.entry("moccasin", "#ffe4b5"),
            Map.entry("navajowhite", "#ffdead"), Map.entry("navy", "#000080"), Map.entry("oldlace", "#fdf5e6"),
            Map.entry("olive", "#808000"), Map.entry("olivedrab", "#6b8e23"), Map.entry("orange", "#ffa500"),
            Map.entry("orangered", "#ff4500"), Map.entry("orchid", "#da70d6"),
            Map.entry("palegoldenrod", "#eee8aa"), Map.entry("palegreen", "#98fb98"),
            Map.entry("paleturquoise", "#afeeee"), Map.entry("palevioletred", "#db7093"),
            Map.entry("papayawhip", "#ffefd5"), Map.entry("peachpuff", "#ffdab9"), Map.entry("peru", "#cd853f"),
            Map.entry("pink", "#ffc0cb"), Map.entry("plum", "#dda0dd"), Map.entry("powderblue", "#b0e0e6"),
            Map.entry("purple", "#800080"), Map.entry("rebeccapurple", "#663399"), Map.entry("red", "#ff0000"),
            Map.entry("rosybrown", "#bc8f8f"), Map.entry("royalblue", "#4169e1"),
            Map.entry("saddlebrown", "#8b4513"), Map.entry("salmon", "#fa8072"), Map.entry("sandybrown", "#f4a460"),
            Map.entry("seagreen", "#2e8b57"), Map.entry("seashell", "#fff5ee"), Map.entry("sienna", "#a0522d"),
            Map.entry("silver", "#c0c0c0"), Map.entry("skyblue", "#87ceeb"), Map.entry("slateblue", "#6a5acd"),
            Map.entry("slategray", "#708090"), Map.entry("slategrey", "#708090"), Map.entry("snow", "#fffafa"),
            Map.entry("springgreen", "#00ff7f"), Map.entry("steelblue", "#4682b4"), Map.entry("tan", "#d2b48c"),
            Map.entry("teal", "#008080"), Map.entry("thistle", "#d8bfd8"), Map.entry("tomato", "#ff6347"),
            Map.entry("turquoise", "#40e0d0"), Map.entry("violet", "#ee82ee"), Map.entry("wheat", "#f5deb3"),
            Map.entry("white", "#ffffff"), Map.entry("whitesmoke", "#f5f5f5"), Map.entry("yellow", "#ffff00"),
            Map.entry("yellowgreen", "#9acd32"), Map.entry("transparent", "#00000000"));

    static ResolvedColor parse(String colorText) {
        return parse(colorText, 0);
    }

    private static ResolvedColor parse(String colorText, int depth) {
        if (colorText == null || depth > MAX_COLOR_MIX_DEPTH) {
            return null;
        }
        var clean = colorText.strip().toLowerCase();
        if (clean.isEmpty()) {
            return null;
        }

        if (clean.startsWith("#")) {
            return HEX.matcher(clean).matches() ? parseHex(clean) : null;
        }

        var named = NAMED.get(clean);
        if (named != null) {
            return parseHex(named);
        }

        var func = FUNC.matcher(clean);
        if (!func.matches()) {
            return null;
        }
        var name = func.group(1).toLowerCase();
        var args = tokenizeArgs(func.group(2).strip());

        return switch (name) {
            case "rgb", "rgba" -> parseRgb(args);
            case "hsl", "hsla" -> parseHsl(args);
            case "hwb" -> parseHwb(args);
            case "lab" -> parseLab(args);
            case "lch" -> parseLch(args);
            case "oklab" -> parseOklab(args);
            case "oklch" -> parseOklch(args);
            case "color-mix" -> parseColorMix(clean, depth);
            default -> null;
        };
    }

    private static ResolvedColor parseHex(String hexText) {
        var hex = hexText;
        if (hex.length() == 4) {
            hex = "#" + repeat(hex, 1) + repeat(hex, 2) + repeat(hex, 3);
        } else if (hex.length() == 5) {
            hex = "#" + repeat(hex, 1) + repeat(hex, 2) + repeat(hex, 3) + repeat(hex, 4);
        }
        var r = Integer.parseInt(hex, 1, 3, 16);
        var g = Integer.parseInt(hex, 3, 5, 16);
        var b = Integer.parseInt(hex, 5, 7, 16);
        Double a = hex.length() == 9 ? Integer.parseInt(hex, 7, 9, 16) / 255.0 : null;
        return makeResult(r, g, b, a);
    }

    private static String repeat(String hex, int index) {
        var c = hex.charAt(index);
        return "" + c + c;
    }

    private static ResolvedColor makeResult(int r, int g, int b, Double a) {
        var hex = "#%02x%02x%02x".formatted(r, g, b);
        if (a != null && a < 1) {
            hex += "%02x".formatted(Math.round(a * 255));
        }
        return new ResolvedColor(hex, r, g, b, a, luminance(r, g, b));
    }

    private static double luminance(int r, int g, int b) {
        return 0.2126 * linearize(r) + 0.7152 * linearize(g) + 0.0722 * linearize(b);
    }

    private static double linearize(int channel) {
        var s = channel / 255.0;
        return s <= 0.03928 ? s / 12.92 : Math.pow((s + 0.055) / 1.055, 2.4);
    }

    private static ResolvedColor parseRgb(List<String> args) {
        if (args.size() != 3 && args.size() != 4) {
            return null;
        }
        var r = percentOrNumber(args.get(0), 255);
        var g = percentOrNumber(args.get(1), 255);
        var b = percentOrNumber(args.get(2), 255);
        var a = args.size() == 4 ? alpha(args.get(3)) : 1;
        if (anyNaN(r, g, b, a)) {
            return null;
        }
        return makeResult(clampChannel(r), clampChannel(g), clampChannel(b), clamp01(a));
    }

    private static ResolvedColor parseHsl(List<String> args) {
        if (args.size() != 3 && args.size() != 4) {
            return null;
        }
        var h = hue(args.get(0));
        var s = percentOrNumber(args.get(1), 1);
        var l = percentOrNumber(args.get(2), 1);
        var a = args.size() == 4 ? alpha(args.get(3)) : 1;
        if (anyNaN(h, s, l, a)) {
            return null;
        }
        var rgb = hslToRgb(h, s, l);
        return makeResult(rgb[0], rgb[1], rgb[2], a);
    }

    private static ResolvedColor parseHwb(List<String> args) {
        if (args.size() != 3 && args.size() != 4) {
            return null;
        }
        var h = hue(args.get(0));
        var w = percentOrNumber(args.get(1), 1);
        var b = percentOrNumber(args.get(2), 1);
        var a = args.size() == 4 ? alpha(args.get(3)) : 1;
        if (anyNaN(h, w, b, a)) {
            return null;
        }
        var rgb = hwbToRgb(h, w, b);
        return makeResult(rgb[0], rgb[1], rgb[2], a);
    }

    private static ResolvedColor parseLab(List<String> args) {
        if (args.size() != 3 && args.size() != 4) {
            return null;
        }
        var l = percentOrNumber(args.get(0), 100);
        var aStar = parseFloat(args.get(1));
        var bStar = parseFloat(args.get(2));
        var a = args.size() == 4 ? alpha(args.get(3)) : 1;
        if (anyNaN(l, aStar, bStar, a)) {
            return null;
        }
        var rgb = labToRgb(l, aStar, bStar);
        return makeResult(rgb[0], rgb[1], rgb[2], a);
    }

    private static ResolvedColor parseLch(List<String> args) {
        if (args.size() != 3 && args.size() != 4) {
            return null;
        }
        var l = percentOrNumber(args.get(0), 100);
        var c = parseFloat(args.get(1));
        var h = hue(args.get(2));
        var a = args.size() == 4 ? alpha(args.get(3)) : 1;
        if (anyNaN(l, c, h, a)) {
            return null;
        }
        var hRad = h * Math.PI / 180;
        var rgb = labToRgb(l, c * Math.cos(hRad), c * Math.sin(hRad));
        return makeResult(rgb[0], rgb[1], rgb[2], a);
    }

    private static ResolvedColor parseOklab(List<String> args) {
        if (args.size() != 3 && args.size() != 4) {
            return null;
        }
        var l = percentOrNumber(args.get(0), 1);
        var aStar = parseFloat(args.get(1));
        var bStar = parseFloat(args.get(2));
        var a = args.size() == 4 ? alpha(args.get(3)) : 1;
        if (anyNaN(l, aStar, bStar, a)) {
            return null;
        }
        var rgb = oklabToRgb(l, aStar, bStar);
        return makeResult(rgb[0], rgb[1], rgb[2], a);
    }

    private static ResolvedColor parseOklch(List<String> args) {
        if (args.size() != 3 && args.size() != 4) {
            return null;
        }
        var l = percentOrNumber(args.get(0), 1);
        var c = parseFloat(args.get(1));
        var h = hue(args.get(2));
        var a = args.size() == 4 ? alpha(args.get(3)) : 1;
        if (anyNaN(l, c, h, a)) {
            return null;
        }
        var hRad = h * Math.PI / 180;
        var rgb = oklabToRgb(l, c * Math.cos(hRad), c * Math.sin(hRad));
        return makeResult(rgb[0], rgb[1], rgb[2], a);
    }

    private static ResolvedColor parseColorMix(String clean, int depth) {
        var subArgs = splitByComma(clean.substring("color-mix(".length(), clean.length() - 1));
        if (subArgs.size() != 3) {
            return null;
        }
        var spaceTokens = subArgs.get(0).strip().split("\\s+");
        if (spaceTokens.length != 2 || !spaceTokens[0].equals("in") || !spaceTokens[1].equals("srgb")) {
            return null;
        }
        var first = colorWithWeight(subArgs.get(1));
        var second = colorWithWeight(subArgs.get(2));
        if (first == null || second == null) {
            return null;
        }
        var c1 = parse(first.colorText(), depth + 1);
        var c2 = parse(second.colorText(), depth + 1);
        if (c1 == null || c2 == null) {
            return null;
        }
        var w1 = first.weight();
        var w2 = second.weight();
        if (w1 == null && w2 == null) {
            w1 = 50.0;
            w2 = 50.0;
        } else if (w1 != null && w2 == null) {
            w2 = 100 - w1;
        } else if (w2 != null && w1 == null) {
            w1 = 100 - w2;
        } else {
            var sum = w1 + w2;
            if (sum == 0) {
                return null;
            }
            w1 = w1 / sum * 100;
            w2 = w2 / sum * 100;
        }
        var f1 = w1 / 100;
        var f2 = w2 / 100;
        var a1 = c1.alpha() != null ? c1.alpha() : 1;
        var a2 = c2.alpha() != null ? c2.alpha() : 1;
        var aMix = a1 * f1 + a2 * f2;
        long r = 0;
        long g = 0;
        long b = 0;
        if (aMix > 0) {
            r = Math.round((c1.r() * a1 * f1 + c2.r() * a2 * f2) / aMix);
            g = Math.round((c1.g() * a1 * f1 + c2.g() * a2 * f2) / aMix);
            b = Math.round((c1.b() * a1 * f1 + c2.b() * a2 * f2) / aMix);
        }
        return makeResult(clampChannel(r), clampChannel(g), clampChannel(b), clamp01(aMix));
    }

    // ── numeric helpers with JS parseFloat semantics ────────────────

    /** JS parseFloat: parses the leading float prefix, NaN when none. */
    static double parseFloat(String text) {
        var matcher = FLOAT_PREFIX.matcher(text.strip());
        return matcher.find() ? Double.parseDouble(matcher.group()) : Double.NaN;
    }

    private static double percentOrNumber(String text, double refMax) {
        var trimmed = text.strip();
        if (trimmed.endsWith("%")) {
            return parseFloat(trimmed.substring(0, trimmed.length() - 1)) / 100 * refMax;
        }
        return parseFloat(trimmed);
    }

    private static double alpha(String text) {
        var trimmed = text.strip();
        if (trimmed.endsWith("%")) {
            return parseFloat(trimmed.substring(0, trimmed.length() - 1)) / 100;
        }
        return parseFloat(trimmed);
    }

    private static double hue(String text) {
        var lower = text.strip().toLowerCase();
        double value;
        if (lower.endsWith("deg")) {
            value = parseFloat(lower.substring(0, lower.length() - 3));
        } else if (lower.endsWith("grad")) {
            value = parseFloat(lower.substring(0, lower.length() - 4)) * 360 / 400;
        } else if (lower.endsWith("rad")) {
            value = parseFloat(lower.substring(0, lower.length() - 3)) * 180 / Math.PI;
        } else if (lower.endsWith("turn")) {
            value = parseFloat(lower.substring(0, lower.length() - 4)) * 360;
        } else {
            value = parseFloat(lower);
        }
        value = value % 360;
        if (value < 0) {
            value += 360;
        }
        return value;
    }

    private static boolean anyNaN(double... values) {
        for (var value : values) {
            if (Double.isNaN(value)) {
                return true;
            }
        }
        return false;
    }

    private static int clampChannel(double value) {
        return (int) Math.max(0, Math.min(255, Math.round(value)));
    }

    private static double clamp01(double value) {
        return Math.max(0, Math.min(1, value));
    }

    // ── argument tokenization ───────────────────────────────────────

    /** Split function arguments: coordinates by space/comma, alpha after a depth-0 '/'. */
    private static List<String> tokenizeArgs(String inner) {
        var slantIndex = -1;
        var depth = 0;
        for (var i = 0; i < inner.length(); i++) {
            var c = inner.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            } else if (depth == 0 && c == '/') {
                slantIndex = i;
                break;
            }
        }
        var coords = slantIndex >= 0 ? inner.substring(0, slantIndex).strip() : inner;
        var args = splitList(coords);
        if (slantIndex >= 0) {
            args.add(inner.substring(slantIndex + 1).strip());
        }
        return args;
    }

    private static List<String> splitList(String text) {
        var results = new ArrayList<String>();
        var current = new StringBuilder();
        var depth = 0;
        for (var i = 0; i < text.length(); i++) {
            var c = text.charAt(i);
            if (c == '(') {
                depth++;
                current.append(c);
            } else if (c == ')') {
                depth--;
                current.append(c);
            } else if (depth == 0 && (c == ',' || Character.isWhitespace(c))) {
                if (!current.toString().strip().isEmpty()) {
                    results.add(current.toString().strip());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }
        if (!current.toString().strip().isEmpty()) {
            results.add(current.toString().strip());
        }
        return results;
    }

    private static List<String> splitByComma(String text) {
        var results = new ArrayList<String>();
        var current = new StringBuilder();
        var depth = 0;
        for (var i = 0; i < text.length(); i++) {
            var c = text.charAt(i);
            if (c == '(') {
                depth++;
                current.append(c);
            } else if (c == ')') {
                depth--;
                current.append(c);
            } else if (depth == 0 && c == ',') {
                results.add(current.toString().strip());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        if (!current.toString().strip().isEmpty()) {
            results.add(current.toString().strip());
        }
        return results;
    }

    record WeightedColor(String colorText, Double weight) {
    }

    private static WeightedColor colorWithWeight(String subArg) {
        var parts = splitList(subArg.strip());
        if (parts.isEmpty() || parts.size() > 2) {
            return null;
        }
        if (parts.size() == 1) {
            return new WeightedColor(parts.get(0), null);
        }
        var p0 = parts.get(0);
        var p1 = parts.get(1);
        var p0Weight = p0.endsWith("%");
        var p1Weight = p1.endsWith("%");
        if (p0Weight && !p1Weight) {
            return new WeightedColor(p1, parseFloat(p0.substring(0, p0.length() - 1)));
        }
        if (p1Weight && !p0Weight) {
            return new WeightedColor(p0, parseFloat(p1.substring(0, p1.length() - 1)));
        }
        return null;
    }

    // ── chromatic conversions ───────────────────────────────────────

    private static int[] hslToRgb(double h, double s, double l) {
        var c = (1 - Math.abs(2 * l - 1)) * s;
        var x = c * (1 - Math.abs((h / 60) % 2 - 1));
        var m = l - c / 2;
        double r;
        double g;
        double b;
        if (h < 60) {
            r = c; g = x; b = 0;
        } else if (h < 120) {
            r = x; g = c; b = 0;
        } else if (h < 180) {
            r = 0; g = c; b = x;
        } else if (h < 240) {
            r = 0; g = x; b = c;
        } else if (h < 300) {
            r = x; g = 0; b = c;
        } else {
            r = c; g = 0; b = x;
        }
        return new int[] {
                clampChannel((r + m) * 255),
                clampChannel((g + m) * 255),
                clampChannel((b + m) * 255) };
    }

    private static int[] hwbToRgb(double h, double w, double b) {
        if (w + b >= 1) {
            var sum = w + b;
            var value = clampChannel(w / sum * 255);
            return new int[] { value, value, value };
        }
        var pure = hslToRgb(h, 1, 0.5);
        return new int[] {
                clampChannel(pure[0] / 255.0 * (1 - w - b) * 255 + w * 255),
                clampChannel(pure[1] / 255.0 * (1 - w - b) * 255 + w * 255),
                clampChannel(pure[2] / 255.0 * (1 - w - b) * 255 + w * 255) };
    }

    private static int[] labToRgb(double l, double a, double b) {
        var fy = (l + 16) / 116;
        var fx = a / 500 + fy;
        var fz = fy - b / 200;

        var e = 216.0 / 24389;
        var k = 24389.0 / 27;

        var fx3 = fx * fx * fx;
        var fz3 = fz * fz * fz;

        var xr = fx3 > e ? fx3 : (116 * fx - 16) / k;
        var yr = l > k * e ? fy * fy * fy : l / k;
        var zr = fz3 > e ? fz3 : (116 * fz - 16) / k;

        // D50 white point, then Bradford adaptation to D65
        var x = xr * 0.96422;
        var y = yr * 1.0;
        var z = zr * 0.82521;

        var x65 = 0.9555726312052288 * x - 0.02303316850884054 * y + 0.06316100215997244 * z;
        var y65 = -0.02828971739420664 * x + 1.0099416310812543 * y + 0.021007716449297163 * z;
        var z65 = 0.012298224741016325 * x - 0.02048298287477757 * y + 1.3299098463422234 * z;

        var rLin = 3.2404542 * x65 - 1.5371385 * y65 - 0.4985314 * z65;
        var gLin = -0.9692660 * x65 + 1.8760108 * y65 + 0.0415560 * z65;
        var bLin = 0.0556434 * x65 - 0.2040259 * y65 + 1.0572252 * z65;

        return new int[] {
                clampChannel(gamma(rLin) * 255),
                clampChannel(gamma(gLin) * 255),
                clampChannel(gamma(bLin) * 255) };
    }

    private static int[] oklabToRgb(double l, double a, double b) {
        var l_ = l + 0.3963377774 * a + 0.2158037573 * b;
        var m_ = l - 0.1055613458 * a - 0.0638541728 * b;
        var s_ = l - 0.0894841775 * a - 1.2914855480 * b;

        var l3 = l_ * l_ * l_;
        var m3 = m_ * m_ * m_;
        var s3 = s_ * s_ * s_;

        var rLin = 4.0767416621 * l3 - 3.3077115913 * m3 + 0.2309699292 * s3;
        var gLin = -1.2684380046 * l3 + 2.6097574011 * m3 - 0.3413193965 * s3;
        var bLin = -0.0041960863 * l3 - 0.7034186147 * m3 + 1.7076147010 * s3;

        return new int[] {
                clampChannel(gamma(rLin) * 255),
                clampChannel(gamma(gLin) * 255),
                clampChannel(gamma(bLin) * 255) };
    }

    private static double gamma(double value) {
        return value <= 0.0031308 ? 12.92 * value : 1.055 * Math.pow(value, 1 / 2.4) - 0.055;
    }
}

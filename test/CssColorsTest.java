import airhacks.zdmd.tokens.control.CssColors;

void main() {
    hexForms();
    namedColors();
    functionalNotations();
    colorMix();
    luminanceAndInvalid();
}

// covers tokens R1.1
void hexForms() {
    var shortHex = CssColors.parse("#abc");
    assert "#aabbcc".equals(shortHex.hex()) : "short hex expands: got " + shortHex.hex();
    var withAlpha = CssColors.parse("#11223380");
    assert "#11223380".equals(withAlpha.hex()) : "alpha hex kept: got " + withAlpha.hex();
    assert Math.abs(withAlpha.alpha() - 128.0 / 255) < 1e-9 : "alpha value: got " + withAlpha.alpha();
}

// covers tokens R1.1
void namedColors() {
    var rebecca = CssColors.parse("rebeccapurple");
    assert "#663399".equals(rebecca.hex()) : "rebeccapurple: got " + rebecca.hex();
    var white = CssColors.parse("WHITE");
    assert "#ffffff".equals(white.hex()) : "case-insensitive names: got " + white.hex();
}

// covers tokens R1.1
void functionalNotations() {
    var rgb = CssColors.parse("rgb(255, 0, 0)");
    assert "#ff0000".equals(rgb.hex()) : "rgb: got " + rgb.hex();
    var modern = CssColors.parse("rgb(255 0 0 / 0.5)");
    assert "#ff000080".equals(modern.hex()) : "rgb with alpha: got " + modern.hex();
    var hsl = CssColors.parse("hsl(120, 100%, 50%)");
    assert "#00ff00".equals(hsl.hex()) : "hsl: got " + hsl.hex();
    var oklch = CssColors.parse("oklch(0.628 0.258 29.23)");
    assert oklch != null && oklch.r() > 200 && oklch.g() < 60 : "oklch red-ish: got " + oklch.hex();
    var hwb = CssColors.parse("hwb(0 0% 0%)");
    assert "#ff0000".equals(hwb.hex()) : "hwb: got " + hwb.hex();
}

// covers tokens R1.1
void colorMix() {
    var even = CssColors.parse("color-mix(in srgb, black, white)");
    assert "#808080".equals(even.hex()) : "even mix: got " + even.hex();
    var weighted = CssColors.parse("color-mix(in srgb, red 30%, blue)");
    assert weighted != null && weighted.b() > weighted.r() : "30/70 mix leans blue: got " + weighted.hex();
    var unsupportedSpace = CssColors.parse("color-mix(in oklab, red, blue)");
    assert unsupportedSpace == null : "only srgb blending supported";
}

// covers tokens R1.1
void luminanceAndInvalid() {
    assert CssColors.parse("#ffffff").luminance() == 1.0 : "white luminance is 1";
    assert CssColors.parse("#000000").luminance() == 0.0 : "black luminance is 0";
    assert CssColors.parse("not-a-color") == null : "invalid color returns null";
    assert CssColors.parse("#12345") == null : "5-digit hex is invalid";
    assert CssColors.parse("rgb(1,2)") == null : "rgb needs 3 or 4 args";
}

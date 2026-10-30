package me.methodial.apothecary.render;

import me.methodial.apothecary.style.FontWeight;

/** A resolved font request. {@code size} is the line box height in px (Minecraft's default font is 9). */
public record FontSpec(String family, float size, FontWeight weight, boolean italic, float letterSpacing) {
    public static final FontSpec DEFAULT = new FontSpec("default", 9, FontWeight.NORMAL, false, 0);

    public FontSpec withSize(float s) { return new FontSpec(family, s, weight, italic, letterSpacing); }
    public FontSpec withWeight(FontWeight w) { return new FontSpec(family, size, w, italic, letterSpacing); }
    public FontSpec withFamily(String f) { return new FontSpec(f, size, weight, italic, letterSpacing); }
}

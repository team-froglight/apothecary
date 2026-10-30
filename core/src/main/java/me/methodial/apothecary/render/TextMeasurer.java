package me.methodial.apothecary.render;

/** Font metrics supplied by the backend. */
public interface TextMeasurer {
    /** Advance width of {@code text} in px at {@code font.size()} (letter spacing included). */
    float width(String text, FontSpec font);

    /** Height of one line box for this font, before line-height scaling (normally font.size()). */
    default float lineHeight(FontSpec font) { return font.size(); }
}

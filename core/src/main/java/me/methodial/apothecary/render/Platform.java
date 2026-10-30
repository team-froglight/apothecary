package me.methodial.apothecary.render;

import me.methodial.apothecary.style.Cursor;

/** Host services a UI root needs besides drawing. */
public interface Platform {
    TextMeasurer text();

    default String clipboard() { return ""; }

    default void setClipboard(String text) { }

    default void setCursor(Cursor cursor) { }

    /** UI feedback sounds: "click", "hover", "toggle", "slide", "open", "close", "error". */
    default void playSound(String id) { }

    /** A platform with no services, measuring text as 6px per character at size 9 (tests, headless layout). */
    static Platform headless() {
        return () -> (text, font) -> text.length() * 6f * font.size() / 9f + Math.max(0, text.length() - 1) * font.letterSpacing();
    }
}

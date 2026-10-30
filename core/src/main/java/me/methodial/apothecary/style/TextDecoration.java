package me.methodial.apothecary.style;

/** text-decoration flags. */
public record TextDecoration(boolean underline, boolean strikethrough) {
    public static final TextDecoration NONE = new TextDecoration(false, false);
}

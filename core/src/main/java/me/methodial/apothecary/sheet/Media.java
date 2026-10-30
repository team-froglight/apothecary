package me.methodial.apothecary.sheet;

/** A condition on the UI root's size, for responsive rules: {@code sheet.media(Media.maxWidth(480), m -> ...)}. */
public record Media(float minWidth, float maxWidth, float minHeight, float maxHeight) {
    public static Media minWidth(float w) { return new Media(w, Float.MAX_VALUE, 0, Float.MAX_VALUE); }
    public static Media maxWidth(float w) { return new Media(0, w, 0, Float.MAX_VALUE); }
    public static Media minHeight(float h) { return new Media(0, Float.MAX_VALUE, h, Float.MAX_VALUE); }
    public static Media maxHeight(float h) { return new Media(0, Float.MAX_VALUE, 0, h); }
    public static Media widthBetween(float min, float max) { return new Media(min, max, 0, Float.MAX_VALUE); }

    /** Both conditions. */
    public Media and(Media o) {
        return new Media(Math.max(minWidth, o.minWidth), Math.min(maxWidth, o.maxWidth),
                Math.max(minHeight, o.minHeight), Math.min(maxHeight, o.maxHeight));
    }

    public boolean matches(float w, float h) { return w >= minWidth && w <= maxWidth && h >= minHeight && h <= maxHeight; }
}

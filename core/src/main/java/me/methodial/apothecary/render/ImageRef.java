package me.methodial.apothecary.render;

/**
 * A backend image. TEXTURE is a whole texture file (u/v span the full image); SPRITE is an atlas
 * sprite (in Minecraft: a GUI sprite, nine-sliced or tiled according to its .mcmeta).
 * {@code width}/{@code height} are the natural size in px, used for intrinsic sizing.
 */
public record ImageRef(String location, Kind kind, int width, int height) {
    public enum Kind { TEXTURE, SPRITE }

    public static ImageRef texture(String location, int width, int height) { return new ImageRef(location, Kind.TEXTURE, width, height); }

    public static ImageRef sprite(String location, int width, int height) { return new ImageRef(location, Kind.SPRITE, width, height); }
}

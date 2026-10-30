package me.methodial.apothecary.theme;

import me.methodial.apothecary.style.Color;

/**
 * Token-aware color math, for values that should follow the theme:
 * <pre>{@code s.bg(alpha(DANGER, 0.13f)).border(1, alpha(DANGER, 0.33f)).fg(DANGER)}</pre>
 */
public final class Colors {
    private Colors() { }

    /** {@code c} with its alpha replaced. */
    public static Ref<Color> alpha(Ref<Color> c, float a) { return t -> c.resolve(t).withAlpha(a); }

    /** Mix of two colors (0 = a, 1 = b). */
    public static Ref<Color> mix(Ref<Color> a, Ref<Color> b, float f) { return t -> a.resolve(t).lerp(b.resolve(t), f); }

    /** Towards white by {@code amount} (0..1). */
    public static Ref<Color> lighten(Ref<Color> c, float amount) { return t -> c.resolve(t).lighten(amount); }

    /** Towards black by {@code amount} (0..1). */
    public static Ref<Color> darken(Ref<Color> c, float amount) { return t -> c.resolve(t).lighten(-amount); }

    /** A constant color as a Ref (for APIs that take Refs). */
    public static Ref<Color> of(Color c) { return t -> c; }

    public static Ref<Color> hex(String hex) { Color c = Color.hex(hex); return t -> c; }

    public static final Ref<Color> TRANSPARENT = t -> Color.TRANSPARENT;
    public static final Ref<Color> WHITE = t -> Color.WHITE;
}

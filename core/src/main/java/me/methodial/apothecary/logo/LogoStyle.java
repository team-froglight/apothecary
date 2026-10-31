package me.methodial.apothecary.logo;

/**
 * The colors of a logo title: a face lit from above ({@code faceTop} → {@code faceMiddle} → {@code faceBottom}),
 * a darker extruded side ({@code sideTop} → {@code sideBottom}) and an {@code outline} around everything.
 * {@code sparkle} scatters small twinkling glints over the faces. A {@code flat} title has only its face and a
 * drop shadow ({@link #flat}). Colors are 0xRRGGBB.
 */
public record LogoStyle(int faceTop, int faceMiddle, int faceBottom, int sideTop, int sideBottom, int outline, boolean sparkle,
                        boolean flat) {
    /** Extruded side, as a share of a block. */
    public static final float DEPTH = 0.7f;
    /** Outline, as a share of a block. */
    public static final float OUTLINE = 0.3f;
    /** How far each block's face swells past its cell, as a share of a block: heavier strokes, tighter counters. */
    public static final float WEIGHT = 0.16f;
    /** A flat title's swell: strokes one and a half blocks thick around counters half a block wide. */
    public static final float FLAT_WEIGHT = 0.25f;

    public LogoStyle(int faceTop, int faceMiddle, int faceBottom, int sideTop, int sideBottom, int outline, boolean sparkle) {
        this(faceTop, faceMiddle, faceBottom, sideTop, sideBottom, outline, sparkle, false);
    }

    public LogoStyle(int faceTop, int faceMiddle, int faceBottom, int sideTop, int sideBottom, int outline) {
        this(faceTop, faceMiddle, faceBottom, sideTop, sideBottom, outline, false);
    }

    public LogoStyle(int faceTop, int faceMiddle, int faceBottom, int sideTop, int sideBottom) {
        this(faceTop, faceMiddle, faceBottom, sideTop, sideBottom, 0x111111, false);
    }

    /** The silver stone of the Minecraft logo. */
    public static final LogoStyle STONE = new LogoStyle(0xFFFFFF, 0xD6D6D6, 0xA9A9A9, 0x6C6C6C, 0x474747);
    /** Bright grass green. */
    public static final LogoStyle GRASS = new LogoStyle(0xD4FF8A, 0x8EDB3C, 0x4E9E22, 0x2F6B16, 0x1D470C, 0x0B1A05);
    /** Icy diamond blue with sparkles. */
    public static final LogoStyle DIAMOND = new LogoStyle(0xC8FBFF, 0x52DDF7, 0x2A9FE0, 0x1A5DA6, 0x0F3A73, 0x050E1F, true);
    /** Glowing gold. */
    public static final LogoStyle GOLD = new LogoStyle(0xFFF6B0, 0xFFD94A, 0xF0A020, 0x95551A, 0x5E300C, 0x1E1003);
    /** Fire: amber to orange. */
    public static final LogoStyle FIRE = new LogoStyle(0xFFE07A, 0xFFA02E, 0xF0581E, 0x8A2A12, 0x55160A, 0x1A0603);
    /** Clear sky: blue to pale lavender. */
    public static final LogoStyle SKY = new LogoStyle(0x5AA2F2, 0x8FBBF7, 0xDCD6FF, 0x2C4E9A, 0x1A2E63, 0x070D1F);
    /** Redstone red. */
    public static final LogoStyle REDSTONE = new LogoStyle(0xFFC0B5, 0xFF5A4A, 0xD42A20, 0x701410, 0x420A08, 0x1A0403);
    /** Amethyst purple with sparkles. */
    public static final LogoStyle AMETHYST = new LogoStyle(0xF2D6FF, 0xC48AF5, 0x8E54D6, 0x50268C, 0x30145C, 0x10061F, true);

    public LogoStyle withSparkle(boolean on) {
        return new LogoStyle(faceTop, faceMiddle, faceBottom, sideTop, sideBottom, outline, on, flat);
    }

    /**
     * A flat title, as Ore UI draws headings and green button labels: one {@code face} color, heavy strokes and a thin
     * drop shadow in {@code shadow}, without the outline and extruded side.
     */
    public static LogoStyle flat(int face, int shadow) {
        return new LogoStyle(face, face, face, shadow, shadow, shadow, false, true);
    }

    /** A style built around one 0xRRGGBB color. */
    public static LogoStyle of(int rgb) {
        return new LogoStyle(mix(rgb, 0xFFFFFF, 0.6f), mix(rgb, 0xFFFFFF, 0.15f), mix(rgb, 0, 0.18f),
                mix(rgb, 0, 0.55f), mix(rgb, 0, 0.72f), mix(rgb, 0, 0.9f));
    }

    /** RGB {@code a} moved toward {@code b} by {@code t}. */
    public static int mix(int a, int b, float t) {
        int out = 0;
        for (int shift = 0; shift <= 16; shift += 8) {
            int x = (a >> shift) & 0xFF, y = (b >> shift) & 0xFF;
            out |= Math.max(0, Math.min(255, (int) (x + (y - x) * t))) << shift;
        }
        return out;
    }

    /** The face color at height t (0 top … 1 bottom). */
    public int face(float t) {
        return t < 0.5f ? mix(faceTop, faceMiddle, t * 2f) : mix(faceMiddle, faceBottom, (t - 0.5f) * 2f);
    }
}

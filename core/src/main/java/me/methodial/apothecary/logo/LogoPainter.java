package me.methodial.apothecary.logo;

import me.methodial.apothecary.geom.Affine;
import me.methodial.apothecary.render.Canvas;

import java.util.Arrays;
import java.util.List;

/**
 * Draws logo titles ({@link LogoFont} block letters in a {@link LogoStyle}) on whole device pixels, so every
 * block edge is crisp at any size and scale. Layer by layer across the whole title: the outline, the sides
 * extruded straight down, the gradient faces, their bevelled edges, then sparkles, all in one batched draw.
 */
public final class LogoPainter {
    /**
     * Per-letter animation by letter index (spaces included): whether the letter is out yet, and its
     * vertical offset in local units. Letters are always solid; the title fades only as a whole.
     */
    public interface Motion {
        boolean shown(int letter);

        float dy(int letter);
    }

    /** Outline plus face swell on each side, in blocks. */
    private static final float PAD = LogoStyle.OUTLINE + LogoStyle.WEIGHT;

    private LogoPainter() { }

    private static boolean flat(List<LogoRun> runs) { return !runs.isEmpty() && runs.getFirst().style().flat(); }

    /** Width of the title in local units, outline (or a flat title's swell) included. */
    public static float width(List<LogoRun> runs, float unit) {
        return (LogoFont.width(text(runs)) + 2 * (flat(runs) ? LogoStyle.FLAT_WEIGHT : PAD)) * unit;
    }

    /** Height of a title in local units, extruded side and outline included. */
    public static float height(float unit) {
        return (LogoFont.HEIGHT + LogoStyle.DEPTH + 2 * PAD) * unit;
    }

    /** Height of these runs in local units: a flat title has no side or outline, only its swell. */
    public static float height(List<LogoRun> runs, float unit) {
        return flat(runs) ? (LogoFont.HEIGHT + 2 * LogoStyle.FLAT_WEIGHT) * unit : height(unit);
    }

    /** Block size at which the title fits {@code maxWidth}, capped at {@code unit}. */
    public static float fit(List<LogoRun> runs, float unit, float maxWidth) {
        float w = width(runs, unit);
        return w <= maxWidth || w <= 0 ? unit : unit * maxWidth / w;
    }

    /** Letters, spaces included: the indices a {@link Motion} is asked about. */
    public static int letterCount(List<LogoRun> runs) {
        int n = 0;
        for (LogoRun r : runs) n += r.text().length();
        return n;
    }

    private static String text(List<LogoRun> runs) {
        StringBuilder b = new StringBuilder();
        for (LogoRun r : runs) b.append(r.text());
        return b.toString();
    }

    public static void paint(Canvas c, List<LogoRun> runs, float x, float y, float unit) {
        paint(c, runs, x, y, unit, null, 1, 0);
    }

    /**
     * Draws the title with its top-left corner (outline included) at (x, y). Below full {@code opacity} it is
     * drawn as one group ({@link Canvas#beginGroup}) so overlapping blocks don't show through as a grid.
     * {@code time} (seconds) drives the sparkles.
     */
    public static void paint(Canvas c, List<LogoRun> runs, float x, float y, float unit, Motion motion, float opacity, float time) {
        if (opacity <= 0.01f || runs.isEmpty()) return;
        boolean group = opacity < 0.995f;
        if (group) c.beginGroup(opacity);
        try {
            paintSolid(c, runs, x, y, unit, motion, time);
        } finally {
            if (group) c.endGroup();
        }
    }

    // ------------------------------------------------------------------ device-pixel drawing

    /** Rects in device pixels, mapped back to local space when flushed. */
    static final class Batch {
        float[] rects = new float[256];
        int[] colors = new int[128];
        int n;

        void add(int x0, int y0, int x1, int y1, int top, int bottom) {
            if (x1 <= x0 || y1 <= y0 || ((top | bottom) >>> 24) == 0) return;
            if (n * 4 + 4 > rects.length) {
                rects = Arrays.copyOf(rects, rects.length * 2);
                colors = Arrays.copyOf(colors, colors.length * 2);
            }
            rects[n * 4] = x0;
            rects[n * 4 + 1] = y0;
            rects[n * 4 + 2] = x1;
            rects[n * 4 + 3] = y1;
            colors[n * 2] = top;
            colors[n * 2 + 1] = bottom;
            n++;
        }

        void add(int x0, int y0, int x1, int y1, int color) { add(x0, y0, x1, y1, color, color); }

        void flush(Canvas c, Affine toLocal) {
            for (int i = 0; i < n; i++) {
                float ax = rects[i * 4], ay = rects[i * 4 + 1], bx = rects[i * 4 + 2], by = rects[i * 4 + 3];
                rects[i * 4] = toLocal.mapX(ax, ay);
                rects[i * 4 + 1] = toLocal.mapY(ax, ay);
                rects[i * 4 + 2] = toLocal.mapX(bx, by);
                rects[i * 4 + 3] = toLocal.mapY(bx, by);
            }
            if (n > 0) c.fillRects(rects, colors, n);
            n = 0;
        }
    }

    private static int argb(int alpha, int rgb) { return (alpha << 24) | (rgb & 0xFFFFFF); }

    private static float hash(int a, int b, int c) {
        int h = a * 73856093 ^ b * 19349663 ^ c * 83492791;
        h ^= h >>> 13;
        h *= 0x5bd1e995;
        h ^= h >>> 15;
        return (h & 0xFFFF) / 65535f;
    }

    private static void paintSolid(Canvas c, List<LogoRun> runs, float x, float y, float unit, Motion motion, float time) {
        int count = letterCount(runs);
        char[] chars = new char[count];
        LogoStyle[] styles = new LogoStyle[count];
        int[] blockX = new int[count];
        int bx = 0, k = 0;
        for (LogoRun run : runs) {
            for (int i = 0; i < run.text().length(); i++) {
                chars[k] = run.text().charAt(i);
                styles[k] = run.style();
                blockX[k] = bx;
                bx += LogoFont.width(chars[k]) + LogoFont.SPACING;
                k++;
            }
        }

        Affine dev = c.deviceMatrix();
        float ps = dev.approxScale();
        int up = Math.max(1, Math.round(unit * ps));
        boolean flat = flat(runs);
        int op = flat ? 0 : Math.max(1, Math.round(up * LogoStyle.OUTLINE));
        int dp = flat ? Math.max(1, Math.round(ps * 0.5f)) : Math.max(1, Math.round(up * LogoStyle.DEPTH));
        int bevel = Math.max(1, Math.round(up * 0.14f));
        // A flat title keeps its counters open: at small sizes the swell gives way before they close.
        int grow = flat ? Math.min(Math.round(up * LogoStyle.FLAT_WEIGHT), (up - 1) / 2) : Math.round(up * LogoStyle.WEIGHT);
        int x0 = Math.round(dev.mapX(x, y)) + op + grow, y0 = Math.round(dev.mapY(x, y)) + op + grow;
        int[] lx = new int[count], ly = new int[count];
        boolean[] shown = new boolean[count];
        for (int i = 0; i < count; i++) {
            lx[i] = x0 + blockX[i] * up;
            ly[i] = y0 + (motion == null ? 0 : Math.round(motion.dy(i) * ps));
            shown[i] = motion == null || motion.shown(i);
        }

        Batch b = new Batch();
        for (int pass = 0; pass < 5; pass++) {
            for (int i = 0; i < count; i++) {
                if (!shown[i] || pass == 4 && !styles[i].sparkle()) continue;
                letter(b, chars[i], styles[i], lx[i], ly[i], up, grow, op, dp, bevel, pass, i, time);
            }
        }
        b.flush(c, dev.invert());
    }

    private static void letter(Batch b, char c, LogoStyle s, int lx, int ly, int up, int grow, int op, int dp, int bevel,
                               int pass, int index, float time) {
        int w = LogoFont.width(c);
        float rows = LogoFont.HEIGHT * up + 2f * grow;
        for (int by = 0; by < LogoFont.HEIGHT; by++) {
            for (int bx = 0; bx < w; bx++) {
                if (!LogoFont.solid(c, bx, by)) continue;
                int cx = lx + bx * up, cy = ly + by * up;
                // The block's face, swollen past its cell so strokes are heavy and neighbours overlap.
                int fx0 = cx - grow, fy0 = cy - grow, fx1 = cx + up + grow, fy1 = cy + up + grow;
                switch (pass) {
                    // A flat title's drop shadow (dp down and right); otherwise one outline around the faces and sides.
                    case 0 -> {
                        if (s.flat()) b.add(fx0 + dp, fy0 + dp, fx1 + dp, fy1 + dp, argb(255, s.sideTop()));
                        else b.add(fx0 - op, fy0 - op, fx1 + op, fy1 + dp + op, argb(255, s.outline()));
                    }
                    // The side, extruded straight down below the face.
                    case 1 -> { if (!s.flat()) b.add(fx0, cy + up / 2, fx1, fy1 + dp, argb(255, s.sideTop()), argb(255, s.sideBottom())); }
                    // The face, lit from above: one smooth gradient down the whole letter.
                    case 2 -> b.add(fx0, fy0, fx1, fy1, argb(255, s.face((fy0 - ly + grow) / rows)),
                            argb(255, s.face((fy1 - ly + grow) / rows)));
                    case 3 -> {
                        if (s.flat()) break;
                        // Bevelled edges: light catches the top and left, the bottom and right fall in shade.
                        // Each edge stops where a neighbour's swollen face begins, so inner corners stay clean.
                        boolean l = LogoFont.solid(c, bx - 1, by), r = LogoFont.solid(c, bx + 1, by);
                        boolean t = LogoFont.solid(c, bx, by - 1), d = LogoFont.solid(c, bx, by + 1);
                        int ex0 = l ? cx + grow : fx0, ex1 = r ? cx + up - grow : fx1;
                        int ey0 = t ? cy + grow : fy0, ey1 = d ? cy + up - grow : fy1;
                        if (!t) b.add(ex0, fy0, ex1, fy0 + bevel, argb(230, LogoStyle.mix(s.faceTop(), 0xFFFFFF, 0.6f)));
                        if (!l) b.add(fx0, ey0, fx0 + bevel, ey1, argb(70, 0xFFFFFF));
                        if (!r) b.add(fx1 - bevel, ey0, fx1, ey1, argb(45, 0));
                        if (!d) b.add(ex0, fy1 - bevel, ex1, fy1, argb(60, 0));
                    }
                    case 4 -> {
                        // Sparkles: a few blocks catch a small twinkling glint.
                        float h = hash(index, bx, by);
                        if (h > 0.3f) continue;
                        int size = Math.max(1, Math.round(up * 0.24f));
                        int room = Math.max(0, up - size - 2 * bevel);
                        int sx = cx + bevel + Math.round(hash(by, index, bx) * room);
                        int sy = cy + bevel + Math.round(hash(bx, by, index) * room);
                        double twinkle = 0.55 + 0.45 * Math.sin(time * 1000 / 260.0 + h * 40);
                        b.add(sx, sy, sx + size, sy + size, argb((int) (255 * twinkle), 0xFFFFFF));
                    }
                    default -> { }
                }
            }
        }
    }
}

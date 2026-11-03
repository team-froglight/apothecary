package me.methodial.apothecary.render;

import me.methodial.apothecary.geom.Affine;
import me.methodial.apothecary.geom.Corners;
import me.methodial.apothecary.geom.Rect;
import me.methodial.apothecary.style.Color;
import me.methodial.apothecary.style.Paint;
import me.methodial.apothecary.style.Shadow;
import me.methodial.apothecary.style.TextDecoration;

/**
 * The drawing surface a backend implements. All coordinates are in the current local space
 * (after {@link #transform}). Colors are straight alpha; the current opacity multiplies everything.
 */
public interface Canvas {
    // ------------------------------------------------------------ state

    /** Saves the transform (clip and opacity have their own stacks). */
    void save();

    void restore();

    /** Post-multiplies the current transform. */
    void transform(Affine m);

    default void translate(float x, float y) { transform(Affine.translation(x, y)); }

    Affine matrix();

    /** Clips to {@code r} (local space; a rotated clip uses its bounding box) intersected with the current clip. */
    void pushClip(Rect r);

    void popClip();

    /** The current clip in canvas (root) space, or null when unclipped. */
    Rect clipBounds();

    void pushOpacity(float alpha);

    void popOpacity();

    float opacity();

    /**
     * Starts a group drawn solid and composited at {@code alpha} as one picture, so overlapping shapes inside
     * don't show through each other while it fades. Backends without offscreen layers fall back to
     * {@link #pushOpacity}. Always pair with {@link #endGroup}.
     */
    default void beginGroup(float alpha) { pushOpacity(alpha); }

    default void endGroup() { popOpacity(); }

    /**
     * Local space → physical pixels. Pixel art (block letters, pixel icons) snaps to this grid so every edge
     * is crisp. Backends that know their display density include it; the default is the transform alone.
     */
    default Affine deviceMatrix() { return matrix(); }

    // ------------------------------------------------------------ shapes

    /** A rounded box with a fill and an optional border drawn inside the edge. */
    void drawBox(Rect r, Corners radii, Paint fill, float borderWidth, Color borderColor);

    default void fillRect(Rect r, Color c) { drawBox(r, Corners.ZERO, Paint.of(c), 0, Color.TRANSPARENT); }

    default void fillRoundRect(Rect r, float radius, Paint p) { drawBox(r, Corners.all(radius), p, 0, Color.TRANSPARENT); }

    default void strokeRoundRect(Rect r, float radius, float width, Color c) { drawBox(r, Corners.all(radius), Paint.NONE, width, c); }

    default void fillCircle(float cx, float cy, float radius, Paint p) {
        drawBox(new Rect(cx - radius, cy - radius, radius * 2, radius * 2), Corners.all(radius), p, 0, Color.TRANSPARENT);
    }

    /** A soft shadow for the box {@code r}. Outer shadows only paint outside the box; inset ones inside. */
    void drawShadow(Rect r, Corners radii, Shadow shadow);

    /** A line segment with round caps. */
    void drawLine(float x0, float y0, float x1, float y1, float width, Color color);

    /** A connected polyline (round joins via capsules). */
    default void drawPolyline(float[] xy, float width, Color color) {
        for (int i = 0; i + 3 < xy.length; i += 2) drawLine(xy[i], xy[i + 1], xy[i + 2], xy[i + 3], width, color);
    }

    /** A ring arc; angles in degrees, 0 = up (12 o'clock), clockwise. */
    void drawArc(float cx, float cy, float radius, float thickness, float startDeg, float sweepDeg, Color color);

    // ------------------------------------------------------------ text, images, effects

    TextMeasurer text();

    /** Draws one line of text with its line box top-left at (x, y). */
    void drawText(String text, float x, float y, FontSpec font, Color color, boolean shadow, TextDecoration decoration);

    void drawImage(ImageRef image, Rect dst, float u0, float v0, float u1, float v1, Color tint);

    default void drawImage(ImageRef image, Rect dst, Color tint) { drawImage(image, dst, 0, 0, 1, 1, tint); }

    /** Runs an effect shader registered with the backend under {@code shaderId} over {@code r}. */
    void drawShader(String shaderId, Rect r, ShaderParams params);

    /**
     * Axis-aligned rectangles with vertical gradients, batched into one draw: {@code rects} holds
     * x0, y0, x1, y1 per rect in local units; {@code colors} holds top and bottom ARGB per rect. No
     * antialiasing, so edges on whole device pixels are exact. For pixel art and blocky shapes.
     */
    default void fillRects(float[] rects, int[] colors, int count) {
        for (int i = 0; i < count; i++) {
            Rect r = new Rect(rects[i * 4], rects[i * 4 + 1], rects[i * 4 + 2] - rects[i * 4], rects[i * 4 + 3] - rects[i * 4 + 1]);
            Color top = Color.ofArgb(colors[i * 2]), bottom = Color.ofArgb(colors[i * 2 + 1]);
            drawBox(r, Corners.ZERO, top.equals(bottom) ? Paint.of(top) : Paint.linear(180, top, bottom), 0, Color.TRANSPARENT);
        }
    }

    /** True when the backend has a shader registered under this id. */
    boolean hasShader(String shaderId);

    /** Backend-specific access (e.g. Minecraft's GuiGraphics), or null. */
    <T> T backend(Class<T> type);

    /** Flushes batched geometry so backend-specific drawing composites in order. */
    default void flush() { }

    /**
     * Blurs everything drawn so far, so what follows sits over a frosted backdrop. Whole-frame: meant for layers that
     * cover the screen. Backends without blur do nothing.
     */
    default void blurBehind() { }
}

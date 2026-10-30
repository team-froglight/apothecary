package me.methodial.apothecary.widget;

import me.methodial.apothecary.geom.Rect;
import me.methodial.apothecary.node.Node;
import me.methodial.apothecary.render.Canvas;
import me.methodial.apothecary.style.Props;

/**
 * A loading spinner: a rotating arc whose length breathes. Color = {@code color}, ring thickness =
 * {@code stroke-width}, track = {@code border-color}. Can also show determinate progress
 * ({@link #progress(float)}) as a circular gauge. Type selector: {@code spinner}.
 */
public class Spinner extends Node {
    private float progress = -1;

    public Spinner() { super("spinner"); }

    /** 0..1 shows a static circular gauge; negative = spinning. */
    public Spinner progress(float p) { this.progress = p; return this; }

    @Override public boolean isLeaf() { return true; }

    @Override
    protected float[] measureContent(float availWidth, float availHeight) {
        float s = get(Props.FONT_SIZE) * 1.6f;
        return new float[]{s, s};
    }

    @Override
    protected void paintContent(Canvas c, Rect r) {
        float size = Math.min(r.width(), r.height());
        float thick = Math.max(1, get(Props.STROKE_WIDTH));
        float radius = size / 2 - thick / 2;
        float cx = r.centerX(), cy = r.centerY();
        var track = get(Props.BORDER_COLOR);
        if (!track.isTransparent()) c.drawArc(cx, cy, radius, thick, 0, 360, track);
        if (progress >= 0) {
            c.drawArc(cx, cy, radius, thick, 0, 360 * Math.min(1, progress), computed().color());
            return;
        }
        float t = root() != null ? root().time() : 0;
        float sweep = 70 + 200 * (0.5f + 0.5f * (float) Math.sin(t * 2.6));
        float start = (t * 300 + 120 * (float) Math.sin(t * 1.3)) % 360;
        c.drawArc(cx, cy, radius, thick, start, sweep, computed().color());
    }
}

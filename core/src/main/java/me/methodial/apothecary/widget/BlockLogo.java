package me.methodial.apothecary.widget;

import me.methodial.apothecary.geom.Rect;
import me.methodial.apothecary.logo.LogoPainter;
import me.methodial.apothecary.logo.LogoRun;
import me.methodial.apothecary.logo.LogoStyle;
import me.methodial.apothecary.node.Node;
import me.methodial.apothecary.render.Canvas;

import java.util.List;

/**
 * A logo title: block letters after the Minecraft logo ({@link me.methodial.apothecary.logo.LogoFont}) with
 * bevelled faces, an extruded side and an outline, drawn on whole device pixels. {@code unit} is the block
 * size; with {@code fit} (the default) the title shrinks to its box's width instead of overflowing.
 * {@link #dropIn} drops the letters in one by one. Type selector: {@code block-logo}.
 */
public class BlockLogo extends Node {
    /** Delay between letters and the time each one takes to land, in seconds. */
    public static final float STAGGER = 0.045f, DROP = 0.24f;

    private List<LogoRun> runs;
    private float unit;
    private boolean fit = true;
    private float dropStart = Float.NaN;

    public BlockLogo(String text, LogoStyle style, float unit) {
        this(LogoRun.of(text, style), unit);
    }

    public BlockLogo(List<LogoRun> runs, float unit) {
        super("block-logo");
        this.runs = List.copyOf(runs);
        this.unit = unit;
    }

    public List<LogoRun> runs() { return runs; }

    public BlockLogo runs(List<LogoRun> r) {
        this.runs = List.copyOf(r);
        markLayoutDirty();
        return this;
    }

    public BlockLogo text(String text, LogoStyle style) { return runs(LogoRun.of(text, style)); }

    public float unit() { return unit; }

    public BlockLogo unit(float u) {
        this.unit = u;
        markLayoutDirty();
        return this;
    }

    public BlockLogo fit(boolean f) {
        this.fit = f;
        markLayoutDirty();
        return this;
    }

    /** Drops the letters in one by one (now, or when the logo is first painted if not yet attached). */
    public BlockLogo dropIn() {
        dropStart = root() != null ? root().time() : -1;
        return this;
    }

    @Override public boolean isLeaf() { return true; }

    @Override
    protected float[] measureContent(float availWidth, float availHeight) {
        float u = fit && Float.isFinite(availWidth) && availWidth > 0 ? LogoPainter.fit(runs, unit, availWidth) : unit;
        return new float[]{LogoPainter.width(runs, u), LogoPainter.height(u)};
    }

    /** Letter i's drop: 0 not yet, 1 landed, overshooting a little in between. */
    public static float drop(float t, int i) {
        float local = Math.max(0, Math.min(1, (t - i * STAGGER) / DROP));
        if (local <= 0) return 0;
        float c = 1.9f, u = local - 1;
        return 1 + (c + 1) * u * u * u + c * u * u;
    }

    /** Letters dropping in one by one, {@code t} seconds after the start. */
    public static LogoPainter.Motion dropMotion(float t, float unit) {
        return new LogoPainter.Motion() {
            @Override public boolean shown(int i) { return t >= i * STAGGER; }

            @Override public float dy(int i) { return (1 - drop(t, i)) * -3 * unit; }
        };
    }

    @Override
    protected void paintContent(Canvas c, Rect r) {
        float u = fit ? LogoPainter.fit(runs, unit, r.width()) : unit;
        float w = LogoPainter.width(runs, u), h = LogoPainter.height(u);
        float time = root() != null ? root().time() : 0;
        LogoPainter.Motion motion = null;
        if (!Float.isNaN(dropStart)) {
            if (dropStart < 0) dropStart = time;
            float t = time - dropStart;
            if (t > STAGGER * LogoPainter.letterCount(runs) + DROP) dropStart = Float.NaN;
            else motion = dropMotion(t, u);
        }
        LogoPainter.paint(c, runs, r.x() + (r.width() - w) / 2, r.y() + (r.height() - h) / 2, u, motion, 1, time);
    }

    @Override
    public boolean isAnimating() {
        if (super.isAnimating() || !Float.isNaN(dropStart)) return true;
        for (LogoRun run : runs) if (run.style().sparkle()) return true;
        return false;
    }
}

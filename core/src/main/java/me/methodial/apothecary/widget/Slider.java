package me.methodial.apothecary.widget;

import me.methodial.apothecary.event.EventType;
import me.methodial.apothecary.event.KeyEvent;
import me.methodial.apothecary.event.MouseEvent;
import me.methodial.apothecary.geom.Rect;
import me.methodial.apothecary.node.Node;
import me.methodial.apothecary.style.Length;
import me.methodial.apothecary.style.Props;

import java.util.function.Function;

/**
 * A horizontal slider. Structure: {@code slider > slider-track > slider-fill}, {@code slider-thumb}
 * (absolute) and an optional {@code slider-value} bubble shown while dragging ({@code :dragging}).
 * Drag, click the track, use arrow keys or the mouse wheel.
 */
public class Slider extends ValueWidget<Float> {
    private final float min, max;
    private float step;
    private final Node fill, thumb;
    private Text bubble;
    private Function<Float, String> format = v -> step >= 1 ? String.valueOf(Math.round(v)) : String.format("%.2f", v);

    public Slider(float min, float max, float value) {
        super("slider", value);
        this.min = min;
        this.max = max;
        focusable(true);
        Node track = new Node("slider-track");
        fill = new Node("slider-fill");
        track.add(fill);
        thumb = new Node("slider-thumb");
        add(track, thumb);
        on(EventType.MOUSE_DOWN, e -> { if (e.button() == MouseEvent.LEFT) setFromLocal(e.localX()); });
        on(EventType.DRAG_START, e -> state("dragging", true));
        on(EventType.DRAG, e -> setFromLocal(e.localX()));
        on(EventType.DRAG_END, e -> state("dragging", false));
        on(EventType.MOUSE_UP, e -> state("dragging", false));
        on(EventType.KEY_DOWN, e -> {
            float s = step > 0 ? step : (max - min) / 20f;
            if (e.key() == KeyEvent.KEY_RIGHT || e.key() == KeyEvent.KEY_UP) { value(value + s); e.preventDefault(); }
            if (e.key() == KeyEvent.KEY_LEFT || e.key() == KeyEvent.KEY_DOWN) { value(value - s); e.preventDefault(); }
            if (e.key() == KeyEvent.KEY_HOME) { value(min); e.preventDefault(); }
            if (e.key() == KeyEvent.KEY_END) { value(max); e.preventDefault(); }
        });
        on(EventType.SCROLL, e -> {
            if (!isFocused()) return;
            float s = step > 0 ? step : (max - min) / 50f;
            value(value + Math.signum(e.deltaY()) * s);
            e.preventDefault();
        });
        valueChanged(value, value);
    }

    public Slider(float min, float max) { this(min, max, min); }

    public Slider step(float s) { this.step = s; setValue(value, false); return this; }

    /** Shows a value bubble above the thumb while dragging. */
    public Slider showValue(Function<Float, String> fmt) {
        if (fmt != null) this.format = fmt;
        if (bubble == null) {
            bubble = new Text(format.apply(value));
            bubble.cls("slider-value");
            thumb.add(bubble);
        }
        return this;
    }

    public float min() { return min; }
    public float max() { return max; }
    public float fraction() { return max == min ? 0 : (value - min) / (max - min); }

    private void setFromLocal(float lx) {
        Rect c = contentBox();
        float f = c.width() <= 0 ? 0 : (lx - c.x()) / c.width();
        Float old = value;
        value(min + Math.max(0, Math.min(1, f)) * (max - min));
        if (!old.equals(value)) sound("slide");
    }

    @Override
    protected Float sanitize(Float v) {
        float x = Math.max(min, Math.min(max, v == null ? min : v));
        if (step > 0) x = min + Math.round((x - min) / step) * step;
        return Math.max(min, Math.min(max, x));
    }

    @Override
    protected void valueChanged(Float old, Float now) {
        fill.style().set(Props.WIDTH, Length.percent(fraction() * 100));
        placeThumb();
        if (bubble != null) bubble.text(format.apply(now));
    }

    /** Centers the thumb on the value's point along the track (content box). */
    private void placeThumb() {
        if (computed() == null) return;
        Rect c = contentBox();
        float left = c.x() + fraction() * c.width() - thumb.width() / 2 - borderEdges().left();
        Length l = Length.px(Math.round(left * 4) / 4f);
        if (!l.equals(thumb.style().get(Props.LEFT))) thumb.style().set(Props.LEFT, l);
    }

    @Override
    protected void onLayout() { placeThumb(); }
}

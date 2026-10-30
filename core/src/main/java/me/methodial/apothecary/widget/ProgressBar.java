package me.methodial.apothecary.widget;

import me.methodial.apothecary.node.Node;
import me.methodial.apothecary.style.Length;
import me.methodial.apothecary.style.Props;

/**
 * A progress bar. {@code progress > progress-fill}; the fill width transitions (see the theme sheet).
 * {@link #indeterminate()} switches to an endless animated stripe ({@code :indeterminate}).
 */
public class ProgressBar extends ValueWidget<Float> {
    private final Node fill;

    public ProgressBar(float value) {
        super("progress", value);
        fill = new Node("progress-fill");
        add(fill);
        valueChanged(value, value);
    }

    public ProgressBar indeterminate() {
        state("indeterminate", true);
        // let the theme sheet size and animate the stripe (an inline width would override it)
        fill.style().remove(Props.WIDTH);
        return this;
    }

    @Override
    protected Float sanitize(Float v) { return v == null ? 0f : Math.max(0, Math.min(1, v)); }

    @Override
    protected void valueChanged(Float old, Float now) {
        state("indeterminate", false);
        fill.style().set(Props.WIDTH, Length.percent(now * 100));
        state("complete", now >= 1f);
    }
}

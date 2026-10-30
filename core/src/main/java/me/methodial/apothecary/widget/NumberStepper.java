package me.methodial.apothecary.widget;

import me.methodial.apothecary.event.EventType;

/** An integer input with − / + buttons. Structure: {@code stepper > button.stepper-dec + text.stepper-value + button.stepper-inc}. */
public class NumberStepper extends ValueWidget<Integer> {
    private final int min, max;
    private int step = 1;
    private final Text label;
    private final Button dec, inc;

    public NumberStepper(int min, int max, int value) {
        super("stepper", value);
        this.min = min;
        this.max = max;
        dec = Button.icon("minus", null);
        dec.cls("stepper-dec", "sm", "ghost");
        inc = Button.icon("plus", null);
        inc.cls("stepper-inc", "sm", "ghost");
        label = new Text(String.valueOf(value));
        label.cls("stepper-value");
        dec.onClick(() -> value(this.value - step));
        inc.onClick(() -> value(this.value + step));
        add(dec, label, inc);
        on(EventType.SCROLL, e -> { value(this.value + (e.deltaY() > 0 ? step : -step)); e.preventDefault(); });
        valueChanged(value, value);
    }

    public NumberStepper step(int s) { this.step = s; return this; }

    @Override
    protected Integer sanitize(Integer v) { return Math.max(min, Math.min(max, v == null ? min : v)); }

    @Override
    protected void valueChanged(Integer old, Integer now) {
        label.text(String.valueOf(now));
        dec.disabled(now <= min);
        inc.disabled(now >= max);
    }
}

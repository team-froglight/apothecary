package me.methodial.apothecary.widget;

import me.methodial.apothecary.event.EventType;
import me.methodial.apothecary.node.Node;

/**
 * A checkbox with a label. Structure: {@code checkbox > checkbox-box > icon.check-mark} + {@code text}.
 * Styles target {@code checkbox:checked}. Supports an indeterminate state ({@code :indeterminate}).
 */
public class Checkbox extends ValueWidget<Boolean> {
    private final Node box;
    private final Icon mark;
    private final Text label;

    public Checkbox(String text) { this(text, false); }

    public Checkbox(String text, boolean checked) {
        super("checkbox", false);
        focusable(true);
        box = new Node("checkbox-box");
        mark = new Icon("check");
        mark.cls("check-mark");
        box.add(mark);
        label = new Text(text);
        label.cls("checkbox-label");
        if (text == null || text.isEmpty()) label.style(s -> s.hidden());
        add(box, label);
        on(EventType.CLICK, e -> toggle());
        setValue(checked, false);
        valueChanged(false, checked);
    }

    public boolean checked() { return Boolean.TRUE.equals(value); }

    public Checkbox toggle() {
        state("indeterminate", false);
        mark.name("check");
        setValue(!checked(), true);
        sound("toggle");
        return this;
    }

    public Checkbox indeterminate(boolean on) {
        state("indeterminate", on);
        mark.name(on ? "minus" : "check");
        return this;
    }

    public Text label() { return label; }

    @Override
    protected void valueChanged(Boolean old, Boolean now) {
        state("checked", Boolean.TRUE.equals(now));
    }
}

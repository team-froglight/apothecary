package me.methodial.apothecary.widget;

import me.methodial.apothecary.event.EventType;
import me.methodial.apothecary.node.Node;

/**
 * An on/off toggle. Structure: {@code switch > switch-track > switch-knob} + optional {@code text}.
 * The knob slides via the theme sheet (a checked-state rule translates {@code switch-knob}, with a transition).
 */
public class Switch extends ValueWidget<Boolean> {
    private final Text label;

    public Switch(String text) { this(text, false); }

    public Switch(String text, boolean on) {
        super("switch", false);
        focusable(true);
        Node track = new Node("switch-track");
        Node knob = new Node("switch-knob");
        track.add(knob);
        label = new Text(text == null ? "" : text);
        label.cls("switch-label");
        if (text == null || text.isEmpty()) label.style(s -> s.hidden());
        add(track, label);
        on(EventType.CLICK, e -> {
            setValue(!isOn(), true);
            sound("toggle");
        });
        setValue(on, false);
        valueChanged(false, on);
    }

    public boolean isOn() { return Boolean.TRUE.equals(value); }

    @Override
    protected void valueChanged(Boolean old, Boolean now) {
        state("checked", Boolean.TRUE.equals(now));
    }
}

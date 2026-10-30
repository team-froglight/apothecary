package me.methodial.apothecary.widget;

import me.methodial.apothecary.event.EventType;
import me.methodial.apothecary.event.KeyEvent;
import me.methodial.apothecary.node.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A group of radio buttons selecting one value. Structure:
 * {@code radio-group > radio > radio-dot > radio-inner} + {@code text}. Arrow keys move the selection.
 */
public class RadioGroup<T> extends ValueWidget<T> {
    private final List<Node> radios = new ArrayList<>();
    private final List<T> values = new ArrayList<>();

    public RadioGroup() {
        super("radio-group", null);
        on(EventType.KEY_DOWN, e -> {
            int idx = values.indexOf(value);
            if (e.key() == KeyEvent.KEY_DOWN || e.key() == KeyEvent.KEY_RIGHT) { select(Math.min(values.size() - 1, idx + 1)); e.preventDefault(); }
            if (e.key() == KeyEvent.KEY_UP || e.key() == KeyEvent.KEY_LEFT) { select(Math.max(0, idx - 1)); e.preventDefault(); }
        });
    }

    public RadioGroup<T> option(T v, String label) {
        Node radio = new Node("radio");
        radio.focusable(true);
        Node dot = new Node("radio-dot").add(new Node("radio-inner"));
        Text t = new Text(label);
        t.cls("radio-label");
        radio.add(dot, t);
        int index = values.size();
        radio.onClick(() -> select(index));
        radios.add(radio);
        values.add(v);
        add(radio);
        if (value == null) setValue(v, false);
        valueChanged(null, value);
        return this;
    }

    private void select(int i) {
        if (i < 0 || i >= values.size()) return;
        if (!Objects.equals(values.get(i), value)) sound("toggle");
        setValue(values.get(i), true);
        if (root() != null && root().focused() != null && root().focused().isDescendantOf(this)) radios.get(i).focus();
    }

    @Override
    protected void valueChanged(T old, T now) {
        for (int i = 0; i < radios.size(); i++) radios.get(i).state("checked", Objects.equals(values.get(i), now));
    }
}

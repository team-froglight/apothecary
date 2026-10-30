package me.methodial.apothecary.widget;

import me.methodial.apothecary.node.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A row of mutually exclusive segments with a sliding pill. Structure:
 * {@code segmented > segment-pill + segment*}; the chosen segment has {@code :selected}.
 */
public class SegmentedControl<T> extends ValueWidget<T> {
    private final List<Node> segments = new ArrayList<>();
    private final List<T> values = new ArrayList<>();
    private final SlidingIndicator pill = new SlidingIndicator("segment-pill");

    public SegmentedControl() {
        super("segmented", null);
        add(pill);
    }

    public SegmentedControl<T> option(T v, String label) {
        Node seg = new Node("segment");
        seg.focusable(true);
        seg.add(new Text(label).cls("segment-label"));
        int idx = values.size();
        seg.onClick(() -> {
            if (!Objects.equals(values.get(idx), value)) sound("click");
            value(values.get(idx));
        });
        segments.add(seg);
        values.add(v);
        add(seg);
        if (value == null) setValue(v, false);
        valueChanged(null, value);
        return this;
    }

    @Override
    protected void valueChanged(T old, T now) {
        for (int i = 0; i < segments.size(); i++) {
            boolean sel = Objects.equals(values.get(i), now);
            segments.get(i).state("selected", sel);
            if (sel) pill.target(segments.get(i));
        }
    }
}

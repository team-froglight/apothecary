package me.methodial.apothecary.widget;

import me.methodial.apothecary.event.EventType;
import me.methodial.apothecary.event.KeyEvent;
import me.methodial.apothecary.node.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Tabs with a spring-animated indicator and animated panel changes. Structure:
 * {@code tabs > tab-bar > tab* + tab-indicator}, {@code tab-panel > (content)}. A newly shown panel
 * gets class {@code enter} plus {@code from-left}/{@code from-right} for direction-aware entrance animations.
 */
public class Tabs extends ValueWidget<Integer> {
    private final Node bar = new Node("tab-bar");
    private final Node panel = new Node("tab-panel");
    private final SlidingIndicator indicator = new SlidingIndicator("tab-indicator").horizontalOnly();
    private final List<Node> tabs = new ArrayList<>();
    private final List<Supplier<Node>> contents = new ArrayList<>();
    private final List<Node> built = new ArrayList<>();
    private boolean cache = true;

    public Tabs() {
        super("tabs", -1);
        bar.add(indicator);
        add(bar, panel);
        bar.on(EventType.KEY_DOWN, e -> {
            if (e.key() == KeyEvent.KEY_RIGHT) { value(Math.min(tabs.size() - 1, value + 1)); tabs.get(value).focus(); e.preventDefault(); }
            if (e.key() == KeyEvent.KEY_LEFT) { value(Math.max(0, value - 1)); tabs.get(value).focus(); e.preventDefault(); }
        });
    }

    public Tabs tab(String label, Node content) { return tab(null, label, () -> content); }

    public Tabs tab(String icon, String label, Supplier<Node> content) {
        Node t = new Node("tab");
        t.focusable(true);
        if (icon != null) t.add(new Icon(icon).cls("tab-icon"));
        t.add(new Text(label).cls("tab-label"));
        int index = tabs.size();
        t.onClick(() -> { if (value != index) sound("click"); value(index); });
        tabs.add(t);
        contents.add(content);
        built.add(null);
        bar.add(t);
        if (value < 0) setValue(0, false);
        valueChanged(-1, value);
        return this;
    }

    /** When false, panels are rebuilt each time they're shown. */
    public Tabs cachePanels(boolean c) { this.cache = c; return this; }

    public Node bar() { return bar; }
    public Node panel() { return panel; }

    @Override
    protected Integer sanitize(Integer v) { return v == null ? 0 : Math.max(0, Math.min(tabs.size() - 1, v)); }

    @Override
    protected void valueChanged(Integer old, Integer now) {
        if (now == null || now < 0 || now >= tabs.size()) return;
        for (int i = 0; i < tabs.size(); i++) tabs.get(i).state("selected", i == now);
        indicator.target(tabs.get(now));
        Node content = cache ? built.get(now) : null;
        if (content == null) {
            content = contents.get(now).get();
            if (cache) built.set(now, content);
        }
        if (old != null && old >= 0) {
            content.removeCls("enter", "from-left", "from-right");
            content.cls("enter", now > old ? "from-right" : "from-left");
            content.replayAnimations();
        }
        panel.setChildren(content);
    }
}

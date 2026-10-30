package me.methodial.apothecary.widget;

import me.methodial.apothecary.event.EventType;
import me.methodial.apothecary.event.MouseEvent;
import me.methodial.apothecary.node.Node;

import java.util.function.Consumer;

/**
 * A push button with an optional leading/trailing icon. Variants via classes:
 * {@code primary}, {@code secondary} (default), {@code ghost}, {@code danger}, {@code outline};
 * sizes {@code sm}, {@code lg}; {@code icon-only}. Type selector: {@code button}.
 */
public class Button extends Node {
    private final Text label;
    private Icon icon, trailing;

    public Button(String text) {
        super("button");
        focusable(true);
        label = new Text(text);
        label.cls("button-label");
        add(label);
        on(EventType.CLICK, e -> { if (root() != null) root().platform().playSound("click"); });
    }

    public Button(String icon, String text) {
        this(text);
        icon(icon);
    }

    public static Button icon(String iconName, String tooltip) {
        Button b = new Button("");
        b.label.style(s -> s.hidden());
        b.icon(iconName);
        b.cls("icon-only");
        if (tooltip != null) b.tooltip(tooltip);
        return b;
    }

    // Covariant overrides keep chains typed: new Button("x").cls("sm").primary()
    @Override public Button cls(String... names) { super.cls(names); return this; }
    @Override public Button style(Consumer<me.methodial.apothecary.style.Style> edit) { super.style(edit); return this; }
    @Override public Button disabled(boolean d) { super.disabled(d); return this; }
    @Override public Button tooltip(String text) { super.tooltip(text); return this; }
    @Override public Button tooltip(java.util.function.Supplier<Node> content) { super.tooltip(content); return this; }

    public Text label() { return label; }

    public Button text(String t) { label.text(t); return this; }

    public Button icon(String name) {
        if (icon == null) {
            icon = new Icon(name);
            icon.cls("button-icon");
            insert(0, icon);
        } else icon.name(name);
        return this;
    }

    public Button trailingIcon(String name) {
        if (trailing == null) {
            trailing = new Icon(name);
            trailing.cls("button-icon", "trailing");
            add(trailing);
        } else trailing.name(name);
        return this;
    }

    /** A leading pixel-art icon ({@link PixelIcon}), for blocky designs. */
    public Button pixelIcon(String name) {
        PixelIcon p = new PixelIcon(name);
        p.cls("button-icon", "pixel");
        insert(0, p);
        return this;
    }

    /** An icon-only button with a pixel-art icon. */
    public static Button pixel(String iconName, String tooltip) {
        Button b = new Button("");
        b.label.style(s -> s.hidden());
        b.pixelIcon(iconName);
        b.cls("icon-only");
        if (tooltip != null) b.tooltip(tooltip);
        return b;
    }

    public Button variant(String v) {
        removeCls("primary", "secondary", "ghost", "danger", "outline", "success", "warning", "gold");
        cls(v);
        return this;
    }

    public Button primary() { return variant("primary"); }
    public Button ghost() { return variant("ghost"); }
    public Button danger() { return variant("danger"); }

    public Button action(Runnable r) { onClick(r); return this; }

    public Button action(Consumer<MouseEvent> r) { onClick(r); return this; }
}

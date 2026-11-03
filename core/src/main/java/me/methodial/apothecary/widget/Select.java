package me.methodial.apothecary.widget;

import me.methodial.apothecary.event.EventType;
import me.methodial.apothecary.event.KeyEvent;
import me.methodial.apothecary.node.UIRoot;
import me.methodial.apothecary.style.Length;
import me.methodial.apothecary.style.Props;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A dropdown picking one of several options. Structure: {@code select > text.select-label + pixel-icon.select-arrow};
 * the open list is a {@link Menu} with class {@code select-menu}; {@code select:open} while open.
 */
public class Select<T> extends ValueWidget<T> {
    private record Option<T>(T value, String label, String icon) { }

    private final List<Option<T>> options = new ArrayList<>();
    private final Text label;
    /** The pixel chevron: down while closed, up while the menu is open. */
    private final PixelIcon arrow = new PixelIcon("chevron-down");
    private Menu menu;
    private String placeholder = "Select…";

    public Select() {
        super("select", null);
        focusable(true);
        label = new Text(placeholder);
        label.cls("select-label");
        arrow.cls("select-arrow");
        add(label, arrow);
        onClick(this::toggle);
        on(EventType.KEY_DOWN, e -> {
            int idx = indexOf(value);
            if (e.key() == KeyEvent.KEY_DOWN && menu == null) { select(Math.min(options.size() - 1, idx + 1)); e.preventDefault(); }
            if (e.key() == KeyEvent.KEY_UP && menu == null) { select(Math.max(0, idx - 1)); e.preventDefault(); }
        });
    }

    public Select<T> option(T v, String text) { return option(v, text, null); }

    public Select<T> option(T v, String text, String icon) {
        options.add(new Option<>(v, text, icon));
        if (value == null) setValue(v, false);
        valueChanged(null, value);
        return this;
    }

    public Select<T> placeholder(String p) { this.placeholder = p; valueChanged(null, value); return this; }

    private int indexOf(T v) {
        for (int i = 0; i < options.size(); i++) if (Objects.equals(options.get(i).value, v)) return i;
        return -1;
    }

    private void select(int i) {
        if (i >= 0 && i < options.size()) setValue(options.get(i).value, true);
    }

    public void toggle() {
        UIRoot r = root();
        if (r == null) return;
        if (menu != null && r.isOpen(menu)) { r.close(menu); return; }
        menu = new Menu();
        menu.cls("select-menu");
        menu.style().set(Props.MIN_WIDTH, Length.px(width()));
        for (int i = 0; i < options.size(); i++) {
            Option<T> o = options.get(i);
            int idx = i;
            menu.item(o.icon, o.label, null, () -> select(idx));
            if (Objects.equals(o.value, value)) menu.items().get(i).state("selected", true);
        }
        state("open", true);
        arrow.name("chevron-up");
        r.showPopup(menu, this, UIRoot.Placement.BELOW, () -> { state("open", false); arrow.name("chevron-down"); menu = null; });
        menu.focus();
        menu.highlight(indexOf(value));
        sound("open");
    }

    @Override
    protected void valueChanged(T old, T now) {
        int i = indexOf(now);
        label.text(i >= 0 ? options.get(i).label : placeholder);
        state("placeholder", i < 0);
    }
}

package me.methodial.apothecary.widget;

import me.methodial.apothecary.event.EventType;
import me.methodial.apothecary.event.KeyEvent;
import me.methodial.apothecary.node.Node;
import me.methodial.apothecary.node.UIRoot;

import java.util.ArrayList;
import java.util.List;

/**
 * A popup list of actions (dropdowns, context menus). Structure:
 * {@code menu > menu-item > [icon] text.menu-label [text.menu-shortcut]} and {@code menu-separator}.
 * Up/Down/Enter navigate when the menu is open.
 */
public class Menu extends Node {
    private final List<Node> items = new ArrayList<>();
    private int highlighted = -1;

    public Menu() {
        super("menu");
        focusable(true);
        on(EventType.KEY_DOWN, e -> {
            if (items.isEmpty()) return;
            if (e.key() == KeyEvent.KEY_DOWN) { highlight((highlighted + 1) % items.size()); e.preventDefault(); }
            else if (e.key() == KeyEvent.KEY_UP) { highlight((highlighted - 1 + items.size()) % items.size()); e.preventDefault(); }
            else if (e.isEnter() && highlighted >= 0) {
                items.get(highlighted).emit(EventType.CLICK, new me.methodial.apothecary.event.MouseEvent(0, 0, 0, 1, 0, 0));
                e.preventDefault();
            }
        });
    }

    public Menu item(String label, Runnable action) { return item(null, label, null, action); }

    public Menu item(String icon, String label, String shortcut, Runnable action) {
        Node it = new Node("menu-item");
        if (icon != null) it.add(new Icon(icon).cls("menu-icon"));
        it.add(new Text(label).cls("menu-label"));
        if (shortcut != null) it.add(new Text(shortcut).cls("menu-shortcut"));
        int index = items.size();
        it.on(EventType.MOUSE_ENTER, e -> highlight(index));
        it.onClick(() -> {
            if (root() != null) root().close(this);
            if (action != null) action.run();
        });
        items.add(it);
        add(it);
        return this;
    }

    public Menu separator() {
        add(new Node("menu-separator"));
        return this;
    }

    public Menu header(String text) {
        add(new Text(text).cls("menu-header"));
        return this;
    }

    public List<Node> items() { return items; }

    public void highlight(int i) {
        for (int k = 0; k < items.size(); k++) items.get(k).state("highlighted", k == i);
        highlighted = i;
        if (i >= 0) items.get(i).scrollIntoView();
    }

    /** Opens next to {@code anchor} (or at the cursor with {@link UIRoot.Placement#CURSOR}). */
    public Menu show(UIRoot root, Node anchor, UIRoot.Placement placement, Runnable onClose) {
        root.showPopup(this, anchor, placement, onClose);
        focus();
        return this;
    }

    /** Opens at the pointer, e.g. from a context-menu handler. */
    public Menu showAtCursor(UIRoot root, Node anchor) { return show(root, anchor, UIRoot.Placement.CURSOR, null); }
}
